<%--
/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>
<style>
 .sub-heading::after {
    border-left: 15px solid transparent;
    border-right: 15px solid transparent;
    border-top: 15px solid #222;
    content: "";
    height: 0;
    left: 45%;
    position: absolute;
    top: 100%;
    width: 0;
}
 .sub-heading {
    background: #222 none repeat scroll 0 0;
    box-sizing: border-box;
    display: table;
    min-height: 58px;
    padding: 5px 50px;
    position: relative;
    width: 100%;
    color:white;
}
.column-adfs-login {
    display: inline-block;
    margin-bottom: 10px;
    margin-left: 14px;
    min-height: 350px;
    position: relative;
    vertical-align: top;
    width: 100%;
    border-left:1px solid #222;
    border-right:1px solid #222;
    border-bottom:1px solid #222;
}
.form.forgot-form {
    padding: 10px;
}
</style>
<%@ include file="/html/portlet/login/init.jsp" %>

<%
User user2 = (User)request.getAttribute(WebKeys.FORGOT_PASSWORD_REMINDER_USER);

if (Validator.isNull(authType)) {
	authType = company.getAuthType();
}

Integer reminderAttempts = (Integer)portletSession.getAttribute(WebKeys.FORGOT_PASSWORD_REMINDER_ATTEMPTS);

if (reminderAttempts == null) {
	reminderAttempts = 0;
}
%>

<portlet:actionURL var="forgotPasswordURL">
	<portlet:param name="struts_action" value="/login/forgot_password" />
</portlet:actionURL>

<div class="row">
   <div class="col-lg-3 col-md-3 col-sm-3">
   </div>
   <div class="col-lg-6 col-md-6 col-sm-6">
   			<div class="column-adfs-login"> 
		        <div class="sub-heading" style="height: 16px;">
					<h3 id="">Forgot Password?</h3>
				</div>

			<aui:form action="<%= forgotPasswordURL %>" cssClass="forgot-form" method="post" name="fm">
				<aui:input name="saveLastPath" type="hidden" value="<%= false %>" />
			
				<portlet:renderURL var="redirectURL" />
			
				<aui:input name="redirect" type="hidden" value="<%= redirectURL %>" />
			
				<liferay-ui:error exception="<%= CaptchaTextException.class %>" message="text-verification-failed" />
			
				<liferay-ui:error exception="<%= NoSuchUserException.class %>" message='<%= "the-" + TextFormatter.format(authType, TextFormatter.K) + "-you-requested-is-not-registered-in-our-database" %>' />
				<liferay-ui:error exception="<%= RequiredReminderQueryException.class %>" message="you-have-not-configured-a-reminder-query" />
				<liferay-ui:error exception="<%= SendPasswordException.class %>" message="your-password-can-only-be-sent-to-an-external-email-address" />
				<liferay-ui:error exception="<%= UserActiveException.class %>" message="your-account-is-not-active" />
				<liferay-ui:error exception="<%= UserEmailAddressException.class %>" message="please-enter-a-valid-email-address" />
				<liferay-ui:error exception="<%= UserReminderQueryException.class %>" message="your-answer-does-not-match-what-is-in-our-database" />
			
				<aui:fieldset>
					<c:choose>
						<c:when test="<%= user2 == null %>">
			
							<%
							String loginParameter = null;
							String loginLabel = null;
			
							if (authType.equals(CompanyConstants.AUTH_TYPE_EA)) {
								loginParameter = "emailAddress";
								loginLabel = "email-address";
							}
							else if (authType.equals(CompanyConstants.AUTH_TYPE_SN)) {
								loginParameter = "screenName";
								loginLabel = "screen-name";
							}
							else if (authType.equals(CompanyConstants.AUTH_TYPE_ID)) {
								loginParameter = "userId";
								loginLabel = "id";
							}
			
							String loginValue = ParamUtil.getString(request, loginParameter);
							%>
			
							<aui:input name="step" type="hidden" value="1" />
							<div class="form-group">  
									<div class="input-group login-input">
										<aui:input cssClass="form-control" label="<%= loginLabel %>" name="<%= loginParameter %>" size="30" type="text" value="<%= loginValue %>">
											<aui:validator name="required" />
										</aui:input>
									</div>
							</div>			
			
							<c:if test="<%= PropsValues.CAPTCHA_CHECK_PORTAL_SEND_PASSWORD %>">
								<portlet:resourceURL var="captchaURL">
									<portlet:param name="struts_action" value="/login/captcha" />
								</portlet:resourceURL>
			
								<liferay-ui:captcha url="<%= captchaURL %>" />
							</c:if>
			
							<aui:button-row>
								<input class="btn btn-danger" type="submit" value='<%= PropsValues.USERS_REMINDER_QUERIES_ENABLED ? "next" : "send-new-password" %>' style="width:200px;margin:30px;" />
								<a href="/c/portal/login"><input type="button" class="btn btn-danger" value="Back To Login" style="width:200px;margin:30px;"/></a>
							</aui:button-row>
			
						</c:when>
						<c:when test="<%= (user2 != null) && Validator.isNotNull(user2.getEmailAddress()) %>">
							<aui:input name="step" type="hidden" value="2" />
							<aui:input name="emailAddress" type="hidden" value="<%= user2.getEmailAddress() %>" />
			
							<c:if test="<%= Validator.isNotNull(user2.getReminderQueryQuestion()) && Validator.isNotNull(user2.getReminderQueryAnswer()) %>">
			
								<%
								String login = null;
			
								if (authType.equals(CompanyConstants.AUTH_TYPE_EA)) {
									login = user2.getEmailAddress();
								}
								else if (authType.equals(CompanyConstants.AUTH_TYPE_SN)) {
									login = user2.getScreenName();
								}
								else if (authType.equals(CompanyConstants.AUTH_TYPE_ID)) {
									login = String.valueOf(user2.getUserId());
								}
								%>
			
								<div class="alert alert-info">
									<%= LanguageUtil.format(pageContext, "a-new-password-will-be-sent-to-x-if-you-can-correctly-answer-the-following-question", login) %>
								</div>
								<div class="form-group">  
									<div class="input-group login-input">
								         <aui:input cssClass="form-control" autoFocus="<%= true %>" label="<%= HtmlUtil.escape(user2.getReminderQueryQuestion()) %>" name="answer" type="text" />
									</div>
								</div>	
							
							</c:if>
			
							<c:choose>
								<c:when test="<%= PropsValues.USERS_REMINDER_QUERIES_REQUIRED && !user2.hasReminderQuery() %>">
									<div class="alert alert-info">
										<liferay-ui:message key="the-password-cannot-be-reset-because-you-have-not-configured-a-reminder-query" />
									</div>
								</c:when>
								<c:otherwise>
									<c:if test="<%= reminderAttempts >= 3 %>">
										<portlet:resourceURL var="captchaURL">
											<portlet:param name="struts_action" value="/login/captcha" />
										</portlet:resourceURL>
			
										<liferay-ui:captcha url="<%= captchaURL %>" />
									</c:if>
			
									<aui:button-row>
										<input class="btn btn-danger" type="submit" value='<%= company.isSendPasswordResetLink() ? "send-password-reset-link" : "send-new-password" %>'style="width:200px;margin:30px;"/>
										<a href="/c/portal/login"><input type="button" class="btn btn-danger" value="Back To Login" style="width:200px;margin:30px;"/></a>
									</aui:button-row>
								</c:otherwise>
							</c:choose>
						</c:when>
						<c:otherwise>
							<div class="alert alert-block">
								<liferay-ui:message key="the-system-cannot-send-you-a-new-password-because-you-have-not-provided-an-email-address" />
							</div>
						</c:otherwise>
					</c:choose>
				</aui:fieldset>
			</aui:form>
		</div>	
   </div>
   <div class="col-lg-3 col-md-3 col-sm-3">
   </div>
</div>

 

<!-- <liferay-util:include page="/html/portlet/login/navigation.jsp" />-->