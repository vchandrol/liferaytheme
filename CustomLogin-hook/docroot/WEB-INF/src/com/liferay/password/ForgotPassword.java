package com.liferay.password;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.compat.portal.util.PortalUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

public class ForgotPassword extends BaseStrutsPortletAction {

	@Override
	public void processAction(StrutsPortletAction originalStrutsPortletAction,
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse) throws Exception {
		System.out.println("--Forgot Password--");

		/*String emailAddress = ParamUtil.getString(actionRequest,
				"forgotpasswordEMail");

		System.out.println("EMail Address: " + emailAddress);

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest
				.getAttribute(WebKeys.THEME_DISPLAY);
		try{
			User user = UserLocalServiceUtil.getUserByEmailAddress(
					themeDisplay.getCompanyId(), emailAddress);
	
			if (Validator.isNotNull(user)) {
				SessionMessages.add(actionRequest, "correct-email");
				actionResponse.setRenderParameter("jspPage", "/html/portlet/login/message.jsp");
			}
		}catch(com.liferay.portal.NoSuchUserException e){
			SessionErrors.add(actionRequest, "wrong-email");
			actionResponse.setRenderParameter("jspPage", "/html/portlet/login/message.jsp");
		}	*/
		originalStrutsPortletAction.processAction(
	            originalStrutsPortletAction, portletConfig, actionRequest,
	            actionResponse);
	}

	@Override
	public String render(StrutsPortletAction originalStrutsPortletAction,
			PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse) throws Exception {

		return originalStrutsPortletAction.render(originalStrutsPortletAction,
				portletConfig, renderRequest, renderResponse);

	}

}