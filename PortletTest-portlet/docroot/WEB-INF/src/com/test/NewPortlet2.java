package com.test;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.ProcessAction;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * Portlet implementation class NewPortlet2
 */
public class NewPortlet2 extends MVCPortlet {
 
	public void updateBook(ActionRequest actionRequest,
			ActionResponse actionResponse)
			throws IOException, PortletException {
			String bookTitle = ParamUtil.getString(actionRequest, "bookTitle");
			String author = ParamUtil.getString(actionRequest, "author");
			System.out.println("Your inputs ==> " + bookTitle + ", " + author);
			}
	
	@ProcessAction(name = "getCategories")
	public void getCategories(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		
		JSONObject parameters = new JSONObject(request.getParameterMap());
		JSONArray list = DataBaseFunctions.getCategories(DataBaseFunctions.getWebConnection());
		
		HttpServletResponse httpResponse = PortalUtil.getHttpServletResponse(response);
		httpResponse.setContentType("text/x-json;charset=UTF-8");
		ServletResponseUtil.write(httpResponse, list.toJSONString());
		
	}
	
	@ProcessAction(name = "getDrugs")
	public void getDrugs(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {

		JSONObject parameters = new JSONObject(request.getParameterMap());
		JSONArray list = DataBaseFunctions.getDrugs(DataBaseFunctions.getWebConnection(),parameters);
		
		
		
		HttpServletResponse httpResponse = PortalUtil.getHttpServletResponse(response);
		httpResponse.setContentType("text/x-json;charset=UTF-8");
		ServletResponseUtil.write(httpResponse, list.toJSONString());
		
	}

	/**
	 * 
	 * @param request
	 *            Possible parameters:<br>
	 *            order_id (int),<br>
	 *            order_start (String/Timestamp: yyyy-[m]m-[d]d hh:mm:ss),<br>
	 *            order_end (String/Timestamp: yyyy-[m]m-[d]d hh:mm:ss),<br>
	 *            order_status (String, one of: 'initiated','sent','delivered','canceled'<br>
	 *            facility_id (int),<br>
	 *            facility_name (String)
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	@ProcessAction(name = "getOrderSummary")
	public void getOrderSummary(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		

		JSONObject parameters = new JSONObject(request.getParameterMap());
		JSONArray list = DataBaseFunctions.getOrderSummary(DataBaseFunctions.getWebConnection(),parameters);
		
		
		HttpServletResponse httpResponse = PortalUtil.getHttpServletResponse(response);
		httpResponse.setContentType("text/x-json;charset=UTF-8");
		ServletResponseUtil.write(httpResponse, list.toJSONString());
		
	}
	
	@ProcessAction(name = "getSentOrderSummary")
	public void getSentOrderSummary(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		
		JSONArray list = new JSONArray();
		for (int i=0; i<3; i++) {
			JSONObject obj=new JSONObject();
			obj.put("date","0"+(i+2)+"/05/2013");
			obj.put("items","3");
			obj.put("status", "arrived");
			list.add(obj);
		}
		
		HttpServletResponse httpResponse = PortalUtil.getHttpServletResponse(response);
		httpResponse.setContentType("text/x-json;charset=UTF-8");
		ServletResponseUtil.write(httpResponse, list.toJSONString());
		
	}
	
	@ProcessAction(name = "getOrderItems")
	public void getOrderItems(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		
		String idParameter = request.getParameter("id");
		JSONArray list = new JSONArray();

		JSONObject obj7=new JSONObject();
		obj7.put("drugname","Afluria");
		obj7.put("unitofissue","100 vials");
		obj7.put("drugform", "vial");
		obj7.put("current", 10);
		obj7.put("price", 100);
		obj7.put("suggested", 11);
		obj7.put("amount", 200);
		obj7.put("id", 4);
		list.add(obj7);
		
		JSONObject obj8=new JSONObject();
		obj8.put("drugname","Gardasil");
		obj8.put("unitofissue","200 vials");
		obj8.put("drugform", "vial");
		obj8.put("current", 30);
		obj8.put("price", 200);
		obj8.put("suggested", 1000);
		obj8.put("amount", 200);
		obj8.put("id", 5);
		list.add(obj8);
		
		JSONObject obj9=new JSONObject();
		obj9.put("drugname","Varivax");
		obj9.put("unitofissue","500 vials");
		obj9.put("drugform", "vial");
		obj9.put("current", 30);
		obj9.put("price", 200);
		obj9.put("suggested", 1000);
		obj9.put("amount", 200);
		obj9.put("id", 6);
		list.add(obj9);
		
		HttpServletResponse httpResponse = PortalUtil.getHttpServletResponse(response);
		httpResponse.setContentType("text/x-json;charset=UTF-8");
		ServletResponseUtil.write(httpResponse, list.toJSONString());
		
	}
	
	@ProcessAction(name = "getSentOrderItems")
	public void getSentOrderItems(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		
		JSONArray list = new JSONArray();
		String idParameter = request.getParameter("id");
				
		JSONObject obj7=new JSONObject();
		obj7.put("drugname","Afluria");
		obj7.put("unitofissue","100 vials");
		obj7.put("drugform", "vial");
		obj7.put("current", 10);
		obj7.put("price", 100);
		obj7.put("suggested", 11);
		obj7.put("amount", 200);
		obj7.put("id", 4);
		list.add(obj7);
		
		JSONObject obj8=new JSONObject();
		obj8.put("drugname","Gardasil");
		obj8.put("unitofissue","200 vials");
		obj8.put("drugform", "vial");
		obj8.put("current", 30);
		obj8.put("price", 200);
		obj8.put("suggested", 1000);
		obj8.put("amount", 200);
		obj8.put("id", 5);
		list.add(obj8);
		
		JSONObject obj9=new JSONObject();
		obj9.put("drugname","Varivax");
		obj9.put("unitofissue","500 vials");
		obj9.put("drugform", "vial");
		obj9.put("current", 30);
		obj9.put("price", 200);
		obj9.put("suggested", 1000);
		obj9.put("amount", 200);
		obj9.put("id", 6);
		list.add(obj9);
		
		HttpServletResponse httpResponse = PortalUtil.getHttpServletResponse(response);
		httpResponse.setContentType("text/x-json;charset=UTF-8");
		ServletResponseUtil.write(httpResponse, list.toJSONString());
		
	}
	
	@ProcessAction(name = "sendOrder")
	public void sendOrder(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		
		System.out.println("Created at: " + request.getParameter("created_at"));
		
	}
	
	@ProcessAction(name = "updateStock")
	public void updateStock(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		
		String added = request.getParameter("added");
		String reduced = request.getParameter("reduced");
		
		if (!(added.equals("NaN"))) {
			System.out.println("Added: " + added);
		}
		
		if (!(reduced.equals("NaN"))) {
			System.out.println("Reduced: " + reduced);
		}
		
	}
	
	
	/*public void serveResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse) {
		
		resourceResponse.setContentType("text/javascript");
		
		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
		jsonObject.put("retVal1", "Returing First value from server");
		
		try {
			PrintWriter writer;
			writer = resourceResponse.getWriter();
			writer.write(jsonObject.toString());
			System.out.println(jsonObject.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
}
