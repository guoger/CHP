package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.dbcp.BasicDataSource;
import org.hsqldb.result.ResultMetaData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.postgresql.PGConnection;
import org.postgresql.ds.PGConnectionPoolDataSource;

public class DataBaseFunctions {

	static final String URL = "localhost";
	static final String USER = "postgres";
	static final String PASSWORD = "postgres";

	static final String SELECT_Order = "SELECT " + "o.id AS Order_ID, "
			+ "o.timestamp AS order_timestamp, " + "o.status AS order_status, "
			+ "f.id AS facility_id, " + "f.name as facility_name, "
			+ "(d.*,c.name,o.unit_number)::drug_ext AS drug ";

	static final String FROM_Order = " FROM facilities f JOIN orders o ON f.id = o.facility_id "
			+ "JOIN drugs d ON o.drug_id = d.id JOIN categories c ON c.id = d.category_id ";

	static final String GET_CATEGORY_NAME = "SELECT c.id AS Category_ID,c.name AS Category_Name FROM categories c";

	static final String ADD_ORDER_START = "WITH meta AS (SELECT ? as fac_id,now() as ts, (?)::order_status as stat) "
			+ "INSERT INTO "
			+ "orders (id,facility_id,drug_id,unit_number,timestamp,status) "
			+ "VALUES ";
	static final String ADD_ORDER_VAL = "(1+(select max(id) from orders),(SELECT fac_id FROM meta),?,?,(SELECT ts FROM meta),(SELECT stat FROM meta))";

	static final String UPDATE_INVENTORY = "SELECT update_inventory(?,?,?)";

	static final String UPDATE_ORDER_STATUS = "UPDATE orders SET status = (?)::order_status"
			+ " WHERE id = ?";
	
	static final String GET_DRUGS = "SELECT * FROM drugs d ";

	private static PGConnectionPoolDataSource dataSourceWeb = null;

	/**
	 * Transforms the rows, received through the given ResultSet, into
	 * JSONObjects and returns them as a JSONArray
	 * 
	 * @param resultSet
	 *            ResultSet to be transformed
	 * @return JSONArray containing JSONObjects
	 * @throws SQLException
	 */
	private static JSONArray resultSetToJSONArray(ResultSet resultSet)
			throws SQLException {

		ResultSetMetaData resultMeta = resultSet.getMetaData();

		int columnNumber = resultMeta.getColumnCount();
		String[] columnNames = new String[columnNumber];
		Integer[] columnTypes = new Integer[columnNumber];
		for (int columnIndex = 1; columnIndex <= columnNumber; columnIndex++) {
			columnNames[columnIndex - 1] = resultMeta
					.getColumnLabel(columnIndex);
			columnTypes[columnIndex - 1] = resultMeta
					.getColumnType(columnIndex);

		}

		JSONArray resultArray = new JSONArray();
		while (resultSet.next()) {
			JSONObject jsonRow = resultSetRowToJSONObject(resultSet);
			resultArray.add(jsonRow);
		}
		return resultArray;

	}

	private static void columnIntoJSONObject(String columnName,
			ResultSet resultSet, int columnType, JSONObject jsonObject)
			throws SQLException {
		switch (columnType) {
		case Types.INTEGER:
			jsonObject.put(columnName, resultSet.getInt(columnName));
			break;
		case Types.TIMESTAMP:
			jsonObject.put(columnName, resultSet.getTimestamp(columnName));
			break;
		case Types.VARCHAR:
		case Types.CHAR:
			jsonObject.put(columnName, resultSet.getString(columnName));
			break;
		case Types.NUMERIC:
		case Types.DOUBLE:
			jsonObject.put(columnName, resultSet.getDouble(columnName));
			break;
		default:
			break;
		}
	}

	private static JSONObject resultSetRowToJSONObject(ResultSet resultSet)
			throws SQLException {
		ResultSetMetaData resultMeta = resultSet.getMetaData();

		int columnNumber = resultMeta.getColumnCount();
		String[] columnNames = new String[columnNumber];
		Integer[] columnTypes = new Integer[columnNumber];
		for (int columnIndex = 1; columnIndex <= columnNumber; columnIndex++) {
			columnNames[columnIndex - 1] = resultMeta
					.getColumnLabel(columnIndex);
			columnTypes[columnIndex - 1] = resultMeta
					.getColumnType(columnIndex);

		}
		for (String name : columnNames)
			System.out.println(name);

		JSONObject jsonRow = new JSONObject();
		for (int columnIndex = 1; columnIndex <= columnNumber; columnIndex++) {
			String columnName = columnNames[columnIndex - 1];

			columnIntoJSONObject(columnName, resultSet,
					columnTypes[columnIndex - 1], jsonRow);

		}
		return jsonRow;
	}

	public static Connection getWebConnection() {
		// TODO
		try {
			if (dataSourceWeb == null) {
				dataSourceWeb = new PGConnectionPoolDataSource();
				dataSourceWeb.setUser(USER);
				dataSourceWeb.setPassword(PASSWORD);
				dataSourceWeb.setServerName(URL);
				dataSourceWeb.setPortNumber(5433);
				dataSourceWeb.setDatabaseName("chpv1");
			}

			Connection con = dataSourceWeb.getPooledConnection()
					.getConnection();
			PGConnection pgCon = (PGConnection) con;
			pgCon.addDataType("drug_ext", Class.forName("com.test.PGDrug"));
			return con;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param con
	 *            Connection to be used
	 * @return JSONArray containing Categories, stored as JSONObjects
	 * @throws SQLException
	 */
	public static JSONArray getCategories(Connection con) {
		ResultSet resultSet;
		JSONArray result = null;
		try {
			resultSet = con.createStatement().executeQuery(GET_CATEGORY_NAME);
			result = resultSetToJSONArray(resultSet);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @param con
	 *            Connection to be used
	 * @param parameters
	 *            JSON Object with the following parameters:<br>
	 *            facility_id : (int),<br>
	 *            status : (String),<br>
	 * <br>
	 *            Additionally Key-Value-Pairs in the form of (drug_id (int) :
	 *            unit_number (int)) will have to be added
	 * @return true if operation succeeded, false otherwise
	 * @throws SQLException
	 */
	public static boolean addOrder(Connection con, JSONObject parameters) {
		if (parameters == null)
			return false;

		Set keySet = parameters.keySet();
		int keySize = keySet.size();

		if (keySize < 3) {
			System.err.println("Not enough Liis, try again!");
			return false;
		}

		Integer facility_id = Integer.valueOf((String) parameters
				.get("facility_id"));
		String status = (String) parameters.get("status");

		if (facility_id == null || status == null)
			return false;

		StringBuilder sb = new StringBuilder();
		sb.append(ADD_ORDER_START);

		int c = 1;
		ArrayDeque<Integer[]> orderNums = new ArrayDeque<>();
		for (Object keyO : keySet) {
			String key = (String) keyO;

			if (!key.isEmpty() && key.matches("[0-9]*")) {
				if (c > 1)
					sb.append(",");
				sb.append(ADD_ORDER_VAL);
				Integer drug_id = Integer.valueOf(key);
				Integer number = Integer.valueOf((String) parameters.get(keyO));
				Integer[] one = { drug_id, number };
				orderNums.add(one);
				System.out.println("Parameters fround: " + drug_id + "|"
						+ number);
				c++;
			}

		}

		PreparedStatement pstmt;
		try {
			pstmt = con.prepareStatement(sb.toString());
			int p = 1;
			pstmt.setInt(p++, facility_id);
			pstmt.setString(p++, status);

			Integer[] orderNum;
			System.out.println("OrderNums size: " + orderNums.size());
			while ((orderNum = orderNums.poll()) != null) {
				pstmt.setInt(p++, orderNum[0]);
				pstmt.setInt(p++, orderNum[1]);
			}
			System.out.println(pstmt.toString());
			pstmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public static JSONArray getDrugs(Connection con, JSONObject parameters) {

		String drug_idS = (String) parameters.get("drug_id");
		String category_idS = (String) parameters.get("category_id");
		
		int p = 0;
		String where = "";
		if (drug_idS != null) {
			if (p==0)
				where += " WHERE ";
			else
				where += " AND ";
			where += "drug_id = ?";
			p++;
		}

		if (category_idS != null) {
			if (p==0)
				where += " WHERE ";
			else
				where += " AND ";
			where += "category_id = ?";
		}
		
		PreparedStatement pstmt;
		try {
			JSONArray result = new JSONArray();
			pstmt = con.prepareStatement(GET_DRUGS + where);
			


			if (drug_idS != null) {
				Integer drug_id = Integer.valueOf(drug_idS);
				pstmt.setInt(1, drug_id);
			}

			if (category_idS != null) {
				Integer category_id = Integer.valueOf(category_idS);
				pstmt.setInt(1, category_id);
			}
			
			ResultSet rs = pstmt.executeQuery();
			
			return resultSetToJSONArray(rs);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return new JSONArray();

	}

	/**
	 * 
	 * @param con
	 *            Connection to be used
	 * @param parameters
	 *            JSON Object with the following parameters:<br>
	 *            order_id (int),<br>
	 *            order_start (Timestamp: yyyy-[m]m-[d]d hh:mm:ss),<br>
	 *            order_end (Timestamp: yyyy-[m]m-[d]d hh:mm:ss),<br>
	 *            order_status (one of:
	 *            'initiated','sent','delivered','canceled'<br>
	 *            facility_id (int),<br>
	 *            facility_name (String)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static JSONArray getOrderSummary(Connection con,
			JSONObject parameters) {
		if (parameters == null)
			return null;

		String order_id = (String) parameters.get("order_id");

		String order_start_String = (String) parameters.get("order_start");
		Timestamp order_start = order_start_String == null ? null
				: java.sql.Timestamp.valueOf(order_start_String);
		String order_end_String = (String) parameters.get("order_end");
		Timestamp order_end = order_end_String == null ? null
				: java.sql.Timestamp.valueOf(order_end_String);
		String order_status = (String) parameters.get("order_status");

		Integer facility_id = Integer.valueOf((String) parameters
				.get("facility_id"));
		String facility_name = (String) parameters.get("facility_name");

		StringBuilder whereBuilder = new StringBuilder("");
		whereBuilder.append(SELECT_Order);
		whereBuilder.append(FROM_Order);

		int c = 0;

		if (order_id != null) {
			whereBuilder.append((c++ > 0 ? " AND " : "WHERE "));
			whereBuilder.append("o.id = ?");
		}

		if (order_start != null) {
			whereBuilder.append((c++ > 0 ? " AND " : "WHERE "));
			whereBuilder.append("o.timestamp >= ?");
		}

		if (order_end != null) {
			whereBuilder.append((c++ > 0 ? " AND " : "WHERE "));
			whereBuilder.append("o.timestamp <= ?");
		}

		if (order_status != null) {
			whereBuilder.append((c++ > 0 ? " AND " : "WHERE "));
			whereBuilder.append("o.status = ?::order_status");
		}

		if (facility_id != null) {
			whereBuilder.append((c++ > 0 ? " AND " : "WHERE "));
			whereBuilder.append("f.id = ?");
		}

		if (facility_name != null) {
			whereBuilder.append((c++ > 0 ? " AND " : "WHERE "));
			whereBuilder.append("f.name LIKE ('%'||?||'%')");
		}

		PreparedStatement pstmt;
		JSONArray resultArray = null;
		try {
			pstmt = con.prepareStatement(whereBuilder.toString()
					+ " ORDER BY o.id ASC");
			int p = 1;

			if (order_id != null)
				pstmt.setInt(p++, Integer.valueOf(order_id));

			if (order_start != null)
				pstmt.setTimestamp(p++, order_start);

			if (order_end != null)
				pstmt.setTimestamp(p++, order_end);

			if (order_status != null)
				pstmt.setString(p++, order_status);

			if (facility_id != null)
				pstmt.setInt(p++, facility_id);

			if (facility_name != null)
				pstmt.setString(p++, facility_name);

			System.out.println(pstmt.toString());

			ResultSet rs = pstmt.executeQuery();

			resultArray = new JSONArray();

			int currentOrderID = -1;

			JSONObject jsonOrder = new JSONObject();
			JSONArray drugs = new JSONArray();
			boolean found_sth = false;
			while (rs.next()) {
				found_sth = true;
				int row_order_id = rs.getInt("order_id");

				if (currentOrderID != row_order_id) {

					if (currentOrderID != -1) {
						jsonOrder.put("drugs", drugs);
						resultArray.add(jsonOrder);
						drugs = new JSONArray();
					}
					jsonOrder = resultSetRowToJSONObject(rs);
					jsonOrder.remove("unit_number");
					// System.out.println(jsonOrder);
					currentOrderID = row_order_id;
				}

				Object drugO = rs.getObject("drug");

				PGDrug drug = (PGDrug) drugO;

				JSONObject jsonDrug = drug.toJSONObject();

				drugs.add(jsonDrug);

			}
			if (found_sth) {
				jsonOrder.put("drugs", drugs);
				resultArray.add(jsonOrder);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return resultArray;
	}

	/**
	 * 
	 * @param con
	 *            Connection to be used
	 * @param parameters
	 *            JSON Object with the following parameters:<br>
	 *            order_id (int),<br>
	 *            status (String),<br>
	 * @return true if operation succeeded, false otherwise
	 * @throws SQLException
	 */
	public static boolean updateOrderStatus(Connection con,
			JSONObject parameters) {
		if (parameters == null)
			return false;
		Integer order_id = Integer.valueOf((String) parameters.get("order_id"));
		String status = (String) parameters.get("status");

		if (order_id == null || status == null)
			return false;

		PreparedStatement pstmt;
		try {
			pstmt = con.prepareStatement(UPDATE_ORDER_STATUS);
			pstmt.setString(1, status);
			pstmt.setInt(2, order_id);

			pstmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;

	}

	/**
	 * 
	 * @param con
	 *            Connection to be used
	 * @param parameters
	 *            JSON Object with the following parameters:<br>
	 *            facility_id : (int),<br>
	 * <br>
	 *            Additionally Key-Value-Pairs in the form of (drug_id (int) :
	 *            difference (int)) will have to be added
	 * @return true if operation succeeded, false otherwise
	 */
	public static boolean updateInventory(Connection con, JSONObject parameters) {
		if (parameters == null)
			return false;

		Integer facility_id = Integer.valueOf((String) parameters
				.get("facility_id"));

		if (facility_id == null)
			return false;

		Set<Map.Entry<Object, Object>> a = parameters.entrySet();

		try {
			PreparedStatement pstmt = con.prepareStatement(UPDATE_INVENTORY);
			for (Iterator<Entry<Object, Object>> iterator = a.iterator(); iterator
					.hasNext();) {
				Entry<Object, Object> entry = iterator.next();
				String key = (String) entry.getKey();
				if (!key.isEmpty() && key.matches("[0-9]*")) {
					pstmt.setInt(1, facility_id);
					pstmt.setInt(2, Integer.valueOf(key));
					pstmt.setInt(3, Integer.valueOf((String) entry.getValue()));
					pstmt.executeQuery();
				}
			}
			return true;
		} catch (SQLException e) {
			e.getNextException().printStackTrace();
			e.printStackTrace();
		}

		return false;

	}

	
	
	private static void testGetDrugs(Connection con) {
		JSONObject input = new JSONObject();
		input.put("category_id", "2");
		JSONArray result = getDrugs(con, input);
		System.out.println(result.toJSONString());
	}
	
	
	public static void main(String[] args) {
		Connection con = getWebConnection();
		testGetDrugs(con);
//		input.put("facility_id", "1");
//		input.put("order_start", "2013-09-21 00:00:00");
		// input.put("drug_common_name", "Asp");
//		JSONArray arr = getOrderSummary(con, input);
//		System.out.println("");
//		JSONObject one = (JSONObject) arr.get(0);
//		JSONArray drugs = (JSONArray) one.get("drugs");
//		JSONObject drug = (JSONObject) drugs.get(0);
//		System.out.println(drug);
//		System.out.println(arr);
		// arr = getCategoryNames(con);

	}

}
