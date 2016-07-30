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
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.HttpUtil"%>
<%@page import="com.liferay.portal.kernel.facebook.FacebookConnectUtil"%>
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
.form.sign-in-form {
    padding: 10px;
}
</style>
<%@ include file="/html/portlet/login/init.jsp" %>

<c:choose>
	<c:when test="<%= themeDisplay.isSignedIn() %>">

		<%
		String signedInAs = HtmlUtil.escape(user.getFullName());

		if (themeDisplay.isShowMyAccountIcon() && (themeDisplay.getURLMyAccount() != null)) {
			String myAccountURL = String.valueOf(themeDisplay.getURLMyAccount());

			if (PropsValues.DOCKBAR_ADMINISTRATIVE_LINKS_SHOW_IN_POP_UP) {
				signedInAs = "<a href=\"javascript:Liferay.Util.openWindow({dialog: {destroyOnHide: true}, title: '" + LanguageUtil.get(pageContext, "my-account") + "', uri: '" + HtmlUtil.escape(myAccountURL) + "'});\">" + signedInAs + "</a>";
			}
			else {
				myAccountURL = HttpUtil.setParameter(myAccountURL, "controlPanelCategory", PortletCategoryKeys.MY);

				signedInAs = "<a href=\"" + HtmlUtil.escape(myAccountURL) + "\">" + signedInAs + "</a>";
			}
		}
		%>

		<%= LanguageUtil.format(pageContext, "you-are-signed-in-as-x", signedInAs, false) %>
	</c:when>
	<c:otherwise>

		<%
		String redirect = ParamUtil.getString(request, "redirect");

		String login = LoginUtil.getLogin(request, "login", company);
		String password = StringPool.BLANK;
		boolean rememberMe = ParamUtil.getBoolean(request, "rememberMe");

		if (Validator.isNull(authType)) {
			authType = company.getAuthType();
		}
		%>

		<portlet:actionURL secure="<%= PropsValues.COMPANY_SECURITY_AUTH_REQUIRES_HTTPS || request.isSecure() %>" var="loginURL">
			<portlet:param name="struts_action" value="/login/login" />
		</portlet:actionURL>
		<div class="row">
		   <div class="col-lg-2 col-md-2 col-sm-2">
		   
		   </div>
		   <div class="col-lg-4 col-md-4 col-sm-4">
		       
		      <div class="column-adfs-login"> 
		        <div class="sub-heading" style="height: 16px;">
					<h3 id="">Technotract Registrants</h3>
				</div>
		        <aui:form action="<%= loginURL %>" autocomplete='<%= PropsValues.COMPANY_SECURITY_LOGIN_FORM_AUTOCOMPLETE ? "on" : "off" %>' cssClass="sign-in-form" method="post" name="fm">
					<aui:input name="saveLastPath" type="hidden" value="<%= false %>" />
					<aui:input name="redirect" type="hidden" value="<%= redirect %>" />
					<aui:input name="doActionAfterLogin" type="hidden" value="<%= portletName.equals(PortletKeys.FAST_LOGIN) ? true : false %>" />
		
					<c:choose>
						<c:when test='<%= SessionMessages.contains(request, "userAdded") %>'>
		
							<%
							String userEmailAddress = (String)SessionMessages.get(request, "userAdded");
							String userPassword = (String)SessionMessages.get(request, "userAddedPassword");
							%>
		
							<div class="alert alert-success">
								<c:choose>
									<c:when test="<%= company.isStrangersVerify() || Validator.isNull(userPassword) %>">
										<%= LanguageUtil.get(pageContext, "thank-you-for-creating-an-account") %>
		
										<c:if test="<%= company.isStrangersVerify() %>">
											<%= LanguageUtil.format(pageContext, "your-email-verification-code-has-been-sent-to-x", userEmailAddress) %>
										</c:if>
									</c:when>
									<c:otherwise>
										<%= LanguageUtil.format(pageContext, "thank-you-for-creating-an-account.-your-password-is-x", userPassword, false) %>
									</c:otherwise>
								</c:choose>
		
								<c:if test="<%= PrefsPropsUtil.getBoolean(company.getCompanyId(), PropsKeys.ADMIN_EMAIL_USER_ADDED_ENABLED) %>">
									<%= LanguageUtil.format(pageContext, "your-password-has-been-sent-to-x", userEmailAddress) %>
								</c:if>
							</div>
						</c:when>
						<c:when test='<%= SessionMessages.contains(request, "userPending") %>'>
		
							<%
							String userEmailAddress = (String)SessionMessages.get(request, "userPending");
							%>
		
							<div class="alert alert-success">
								<%= LanguageUtil.format(pageContext, "thank-you-for-creating-an-account.-you-will-be-notified-via-email-at-x-when-your-account-has-been-approved", userEmailAddress) %>
							</div>
						</c:when>
					</c:choose>
		
					<liferay-ui:error exception="<%= AuthException.class %>" message="authentication-failed" />
					<liferay-ui:error exception="<%= CompanyMaxUsersException.class %>" message="unable-to-login-because-the-maximum-number-of-users-has-been-reached" />
					<liferay-ui:error exception="<%= CookieNotSupportedException.class %>" message="authentication-failed-please-enable-browser-cookies" />
					<liferay-ui:error exception="<%= NoSuchUserException.class %>" message="authentication-failed" />
					<liferay-ui:error exception="<%= PasswordExpiredException.class %>" message="your-password-has-expired" />
					<liferay-ui:error exception="<%= UserEmailAddressException.class %>" message="authentication-failed" />
					<liferay-ui:error exception="<%= UserLockoutException.class %>" message="this-account-has-been-locked" />
					<liferay-ui:error exception="<%= UserPasswordException.class %>" message="authentication-failed" />
					<liferay-ui:error exception="<%= UserScreenNameException.class %>" message="authentication-failed" />
		
					<aui:fieldset>
		
						<%
						String loginLabel = null;
		
						if (authType.equals(CompanyConstants.AUTH_TYPE_EA)) {
							loginLabel = "email-address";
						}
						else if (authType.equals(CompanyConstants.AUTH_TYPE_SN)) {
							loginLabel = "screen-name";
						}
						else if (authType.equals(CompanyConstants.AUTH_TYPE_ID)) {
							loginLabel = "id";
						}
						%>
						<div class="form-group">  
							<div class="input-group login-input">
								<aui:input autoFocus="<%= windowState.equals(LiferayWindowState.EXCLUSIVE) || windowState.equals(WindowState.MAXIMIZED) %>" cssClass="form-control" label="<%= loginLabel %>" name="login" showRequiredLabel="<%= false %>" type="text" value="<%= login %>">
									<aui:validator name="required" />
								</aui:input>
							</div>	
							<div class="input-group login-input">
								<aui:input name="password" cssClass="form-control" showRequiredLabel="<%= false %>" type="password" value="<%= password %>">
									<aui:validator name="required" />
								</aui:input>
							</div>	
			
							<span id="<portlet:namespace />passwordCapsLockSpan" style="display: none;"><liferay-ui:message key="caps-lock-is-on" /></span>
							 <div class="checkbox">
								<c:if test="<%= company.isAutoLogin() && !PropsValues.SESSION_DISABLED %>">
									<aui:input checked="<%= rememberMe %>" name="rememberMe" type="checkbox" />
								</c:if>
							 </div>	
						</div><!-- form-group  -->	 
					</aui:fieldset>
		
					<aui:button-row>
						<input type="submit" class="btn btn-danger" value="sign-in" style="width:100%" />
					</aui:button-row>
				</aui:form>
				<portlet:renderURL var="forgotpassword">
											<portlet:param name="struts_action" value="/login/forgot_password" />
				</portlet:renderURL>
				 <aui:form cssClass="sign-in-form" method="post" name="fm">
				   <a href="<%= forgotpassword%>"><input type="button" value="Forgot Password?" class="btn btn-danger"  style="width:100%"/></a>
				 </aui:form>
				        <%-- <div class="panel-group" id="accordion">
                            <div class="panel panel-default">
                                <div class="panel-heading">
                                    <h4 class="panel-title">
                                        <a class="collapsed" data-toggle="collapse" data-parent="#accordion" href="#collapseOne">
                                            Forgot Password?
                                        </a>
                                    </h4>
                                </div>
                                <div style="height: 0px;" id="collapseOne" class="panel-collapse collapse">
                                    <div class="panel-body">
                                         If you have forgotten your password please enter your username. 
                                         After submitting, you will receive an email that contains a link that leads you to a page where you can reset your password. 
                                         <br/>
                                         <liferay-ui:error key="wrong-email" message="The Email address is not correct"/>
                                         <liferay-ui:success key="correct-email" message="Check your email we have sent reset link."/>
                                         <br/>
                                         
                                         <portlet:actionURL var="forgotpassword">
											<portlet:param name="struts_action" value="/login/forgot_password" />
										</portlet:actionURL>
                                         
                                         <form action="<%=forgotpassword.toString() %>" method="post">
	                                         <div class="row" style="margin-top:15px;">
	                                              <div class="col-lg-8 col-md-8">
	                                                   <input class="form-control" placeholder="Email Address.." type="text" name="<portlet:namespace/>forgotpasswordEMail">
	                                              </div>
	                                              <div class="col-lg-4 col-md-4">
	                                                    <button type="submit" class="btn btn-primary">Submit</button>
	                                              </div>
	                                         </div>
                                         </form>
                                        <br/><br/>
                                       
                                    </div>
                                </div>
                            </div>
                         </div>   --%> 
			     </div>
		   </div>
		   <div class="col-lg-4 col-md-4 col-sm-4">
		   		 <div class="column-adfs-login"> 
			        <div class="sub-heading" style="height: 16px;">
						<h3 id="">Social Registrants</h3>
					</div>             
		   		    <form role="form" class="form sign-in-form">
		   		    
		   		        <portlet:renderURL var="loginRedirectURL" windowState="<%= LiferayWindowState.POP_UP.toString() %>">
							  <portlet:param name="struts_action" value="/login/login_redirect" />
						</portlet:renderURL>
						
						
						<%
							String facebookAuthRedirectURL = FacebookConnectUtil.getRedirectURL(themeDisplay.getCompanyId());
							facebookAuthRedirectURL = HttpUtil.addParameter(facebookAuthRedirectURL, "redirect", HttpUtil.encodeURL(loginRedirectURL.toString()));
									
							String facebookAuthURL = FacebookConnectUtil.getAuthURL(themeDisplay.getCompanyId());
							facebookAuthURL = HttpUtil.addParameter(facebookAuthURL, "client_id", FacebookConnectUtil.getAppId(themeDisplay.getCompanyId()));
							facebookAuthURL = HttpUtil.addParameter(facebookAuthURL, "redirect_uri", facebookAuthRedirectURL);
							facebookAuthURL = HttpUtil.addParameter(facebookAuthURL, "scope", "email");
							String taglibOpenFacebookConnectLoginWindow = "javascript:var facebookConnectLoginWindow = window.open('" + facebookAuthURL.toString() + "','facebook', 'align=center,directories=no,height=560,location=no,menubar=no,resizable=yes,scrollbars=yes,status=no,toolbar=no,width=1000'); void(''); facebookConnectLoginWindow.focus();";
						%>
						<a class="btn btn-social btn-facebook" href="<%= taglibOpenFacebookConnectLoginWindow %>">
												<!-- <i class="fa fa-facebook"></i> -->
												 <img alt="login with facebook" src="/html/portlet/login/facebook.png">
						</a>
						
						 <%
											String googleAuthURL = PortalUtil.getPathContext() + "/c/portal/google_login?cmd=login";
											//String googleAuthURL = PortalUtil.getPathContext() + "/businesshome?cmd=login";
											String taglibOpenGoogleLoginWindow = "javascript:var googleLoginWindow = window.open('" + googleAuthURL.toString() + "', 'facebook', 'align=center,directories=no,height=560,location=no,menubar=no,resizable=yes,scrollbars=yes,status=no,toolbar=no,width=1000'); void(''); googleLoginWindow.focus();";
											
											boolean googleAuthEnabled = PrefsPropsUtil.getBoolean(company.getCompanyId(), "google.auth.enabled", true);
						%>
						
						<a class="btn btn-social btn-google" href="<%= taglibOpenGoogleLoginWindow %>" style="color:white">
								<img alt="login with google" src="/html/portlet/login/google.png">
														  
						</a>
						
                                    <!-- <h4 class="section-title">Login Form</h4>
         
                                    <div class="form-group">
                                        <div class="input-group login-input">
                                            <span class="input-group-addon"><i class="fa fa-user"></i></span>
                                            <input class="form-control" placeholder="Username" type="text">
                                        </div>
                                        <br>
                                        <div class="input-group login-input">
                                            <span class="input-group-addon"><i class="fa fa-lock"></i></span>
                                            <input class="form-control" placeholder="Password" type="password">
                                        </div>
                                        <div class="checkbox">
                                            <label>
                                                <input type="checkbox"> Remember me
                                            </label>
                                        </div>
                                        <button type="submit" class="btn btn-danger pull-right">Login</button>
                                        <div class="clearfix"></div>
                                    </div> -->
                       </form> 
                    </div>       
		   </div>
		   <div class="col-lg-2 col-md-2 col-sm-2">
		   
		   </div>
		</div>
		

		<!--  <liferay-util:include page="/html/portlet/login/navigation.jsp" />-->

		<aui:script use="aui-base">
			var password = A.one('#<portlet:namespace />password');

			if (password) {
				password.on(
					'keypress',
					function(event) {
						Liferay.Util.showCapsLock(event, '<portlet:namespace />passwordCapsLockSpan');
					}
				);
			}
		</aui:script>
	</c:otherwise>
</c:choose>