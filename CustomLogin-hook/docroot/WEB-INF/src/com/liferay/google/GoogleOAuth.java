package com.liferay.google;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.mail.internet.InternetAddress;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import com.liferay.mail.service.MailServiceUtil;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.deploy.DeployManagerUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.struts.BaseStrutsAction;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Contact;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.PortletURLFactoryUtil;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;

public class GoogleOAuth extends BaseStrutsAction {
	public static final String GOOGLE_ACCESS_TOKEN = "googleAccessToken";

	public static final String GOOGLE_REFRESH_TOKEN = "googleRefreshToken";

	public static final String GOOGLE_USER_ID = "googleUserId";
	private static final String _REDIRECT_URI =
	"/c/portal/google_login?cmd=token";
	/*private static final String _REDIRECT_URI =
			"/businesshome?cmd=token";*/

	@Override
	public String execute(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		String cmd = ParamUtil.getString(request, Constants.CMD);

		String redirectUri = PortalUtil.getPortalURL(request) + _REDIRECT_URI;

		if (cmd.equals("login")) {
			GoogleAuthorizationCodeFlow flow = getFlow(
				themeDisplay.getCompanyId());

			GoogleAuthorizationCodeRequestUrl
				googleAuthorizationCodeRequestUrl = flow.newAuthorizationUrl();

			googleAuthorizationCodeRequestUrl.setRedirectUri(redirectUri);

			String url = googleAuthorizationCodeRequestUrl.build();

			response.sendRedirect(url);
		}
		else if (cmd.equals("token")) {
			HttpSession session = request.getSession();

			String code = ParamUtil.getString(request, "code");

			if (Validator.isNotNull(code)) {
				Credential credential = exchangeCode(
					themeDisplay.getCompanyId(), code, redirectUri);

				User user = setGoogleCredentials(
					session, themeDisplay.getCompanyId(), credential);

				if ((user != null) &&
					(user.getStatus() == WorkflowConstants.STATUS_INCOMPLETE)) {

					redirectUpdateAccount(request, response, user);

					return null;
				}

				sendLoginRedirect(request, response);

				return null;
			}

			String error = ParamUtil.getString(request, "error");

			if (error.equals("access_denied")) {
				sendLoginRedirect(request, response);

				return null;
			}
		}

		return null;
	}

	protected User addUser(
			HttpSession session, long companyId, Userinfo userinfo)
		throws Exception {

		long creatorUserId = 0;
		boolean autoPassword = true;
		String password1 = StringPool.BLANK;
		String password2 = StringPool.BLANK;
		boolean autoScreenName = true;
		String screenName = StringPool.BLANK;
		String emailAddress = userinfo.getEmail();
		String openId = StringPool.BLANK;
		Locale locale = LocaleUtil.getDefault();
		String firstName = userinfo.getGivenName();
		String middleName = StringPool.BLANK;
		String lastName = userinfo.getFamilyName();
		int prefixId = 0;
		int suffixId = 0;
		boolean male = Validator.equals(userinfo.getGender(), "male");
		int birthdayMonth = Calendar.JANUARY;
		int birthdayDay = 1;
		int birthdayYear = 1970;
		String jobTitle = StringPool.BLANK;
		long[] groupIds = null;
		long[] organizationIds = null;
		//long[] roleIds = null;

		boolean sendEmail = true;

		/*UserGroup userGroup = null;
		try {
			System.out.println("--Total UserGroup--"
					+ UserGroupLocalServiceUtil.getUserGroupsCount());
			List<UserGroup> list = UserGroupLocalServiceUtil.getUserGroups(
					0, UserGroupLocalServiceUtil.getUserGroupsCount());
			for (UserGroup userGroup2 : list) {
				if (userGroup2.getName().equalsIgnoreCase("General")) {
					userGroup = userGroup2;

					break;
				}
			}

		} catch (Exception e) {
		   System.out.println("--USER GROUP EXCEPTION RAISED--");
		}*/

		Role role=null;
		Role managerUserRole = null;
		Role supportUserRole = null;
		try {
			List<Role> list=RoleLocalServiceUtil.getRoles(0, RoleLocalServiceUtil.getRolesCount());
			for(Role role2:list){
				if(role2.getName().equalsIgnoreCase("Employee")){
					role=role2;
					break;
				}
			}
			list = RoleLocalServiceUtil.getRoles(
					0, RoleLocalServiceUtil.getRolesCount());
			for (Role role2 : list) {
				if (role2.getName().equalsIgnoreCase("Manager")) {
					managerUserRole = role2;

					break;
				}
			}
			list = RoleLocalServiceUtil.getRoles(
					0, RoleLocalServiceUtil.getRolesCount());
			for (Role role2 : list) {
				if (role2.getName().equalsIgnoreCase("Support")) {
					supportUserRole = role2;

					break;
				}
			}
		}catch(Exception e)
		{
			 System.out.println("--ROLE EXCEPTION RAISED--");
		}
		long[] roleIds=new long[1];
		if(role!=null){
			roleIds[0]=role.getRoleId();
		}
		if(managerUserRole!=null){
			
			roleIds[0]=managerUserRole.getRoleId();
		}
		if(supportUserRole!=null){
			roleIds[0]=supportUserRole.getRoleId();
		}
		//long userGroupIds[] = { userGroup.getUserGroupId() };
		
		UserGroup employeeUserGroup = null;
		UserGroup managerUserGroup = null;
		UserGroup supportUserGroup = null;
		try {
			System.out.println("--Total UserGroup--"
					+ UserGroupLocalServiceUtil.getUserGroupsCount());
			List<UserGroup> list = UserGroupLocalServiceUtil.getUserGroups(
					0, RoleLocalServiceUtil.getRolesCount());
			for (UserGroup userGroup2 : list) {
				if (userGroup2.getName().equalsIgnoreCase("Employee")) {
					employeeUserGroup = userGroup2;

					break;
				}
			}
			for (UserGroup userGroup2 : list) {
				if (userGroup2.getName().equalsIgnoreCase("Manager")) {
					managerUserGroup = userGroup2;

					break;
				}
			}
			for (UserGroup userGroup2 : list) {
				if (userGroup2.getName().equalsIgnoreCase("Support")) {
					supportUserGroup = userGroup2;

					break;
				}
			}

		} catch (Exception e) {
		   System.out.println("--USER GROUP EXCEPTION RAISED--");
		}

		long userGroupIds[] = new long[1];
		if(employeeUserGroup!=null){
			userGroupIds[0]=employeeUserGroup.getUserGroupId();
		}

		if(managerUserGroup!=null){
			userGroupIds[0]=managerUserGroup.getUserGroupId();		
		}
		if(supportUserGroup!=null){
			userGroupIds[0]=supportUserGroup.getUserGroupId();
		}
			
			

		ServiceContext serviceContext = new ServiceContext();
		String password=randomString(6);
		User user = UserLocalServiceUtil.addUser(
			creatorUserId, companyId, false, password, password,
			autoScreenName, screenName, emailAddress, 0, openId, locale,
			firstName, middleName, lastName, prefixId, suffixId, male,
			birthdayMonth, birthdayDay, birthdayYear, jobTitle, groupIds,
			organizationIds, roleIds, userGroupIds, sendEmail, serviceContext);

		long userIds[]={user.getUserId()};
		
		if(employeeUserGroup!=null){
			Group group= GroupLocalServiceUtil.getGroup(companyId, "Employee");
			Role roleInfo=RoleLocalServiceUtil.getRole(companyId, "Employee");
			System.out.println(group.getGroupId()+"---ADD EMPLOYEE NEW USER---"+roleInfo.getRoleId());
			UserGroupRoleLocalServiceUtil.addUserGroupRoles(userIds, group.getGroupId(),roleInfo.getRoleId());

		}

		if(managerUserGroup!=null){
			Group group= GroupLocalServiceUtil.getGroup(companyId, "Manager");
			Role roleInfo=RoleLocalServiceUtil.getRole(companyId, "Manager");
			System.out.println(group.getGroupId()+"---ADD MANAGER NEW USER---"+roleInfo.getRoleId());
			UserGroupRoleLocalServiceUtil.addUserGroupRoles(userIds, group.getGroupId(),roleInfo.getRoleId());

		}
		if(supportUserGroup!=null){
			Group group= GroupLocalServiceUtil.getGroup(companyId, "Support");
			Role roleInfo=RoleLocalServiceUtil.getRole(companyId, "Support");
			System.out.println(group.getGroupId()+"---ADD SUPPORT NEW USER---"+roleInfo.getRoleId());
			UserGroupRoleLocalServiceUtil.addUserGroupRoles(userIds, group.getGroupId(),roleInfo.getRoleId());

		}
		
		
		
		user = UserLocalServiceUtil.updateLastLogin(
			user.getUserId(), user.getLoginIP());

		user = UserLocalServiceUtil.updatePasswordReset(
			user.getUserId(), false);

		user = UserLocalServiceUtil.updateEmailAddressVerified(
			user.getUserId(), true);

		session.setAttribute("GOOGLE_USER_EMAIL_ADDRESS", emailAddress);

		MailMessage mailMessage=new MailMessage();
		mailMessage.setHTMLFormat(true);
		mailMessage.setBody("<div style='width:600px;height:650px;background-color: rgba(0, 32, 73, 0.95);'>"
                             +"<div style='height:100px;border-bottom:2px solid #00948f'>" +
                             "<img src='"+"././images/img1.png' style='width:100px;height:50px;' />"+"</div>"
                             +"<div style='height:450px;padding:20px;color:white;'>"
                             +"Hello <strong>"+firstName+" "+lastName+"</strong>,<br/><br/>"
                             +"<span>Welcome to the jobengine.com. Thanks for opening an account with us!!!</span><br/>"
                             +"<span>First thing first..</span><br/><br/>"
                             +"<ul style='list-style-type:none;text-decoration:none;'>"
					         +"<li style='float:left;'><strong>Your UserName Is:</strong><li>"+emailAddress+"<br/>"
                             +"<li style='float:left;'><strong>Your Password Is:</strong><li>"+password
                             +"</ul> <br/><br/>"
                             +"<span>To change your details, including your newsletter subscription and password simply "
                             +"complete your registration process and login into the account.</span><br/><br/>"
                             +"<strong>Let the fun begin!</strong><br/><br/>"
                             +"<span>Get ready to have some serious fun with our fantastic range</span><br/><br/>"
                             +"<strong> Good luck!<br/>JobEngine Support Team</strong><br/><br/>"
                             +"<span style=''><u>http://localhost:8080</u></span><br/>"
							 +"<strong>Free Help & Support</strong><br/>"
							 +"<strong>Email:</strong><u>support@jobengine.in</u><br/>"
							 +"<strong><u>FAQ</u></strong>	<br/><br/><br/>"
							 +"<span>*Conditions Apply</span><br/><br/>"
							 +"<small>Users must be 18 or over to register this site."
							 +"Head Office: JobEngine India Network Ltd.1 35,2nd Floor, Near Railway Station, Jabalpur - 400 018</small> </div></div>");

		mailMessage.setFrom(new InternetAddress("vchandrol@gmail.com","Vikash Kumar"));
		mailMessage.setSubject("Thank you for your registration");
		mailMessage.setTo(new InternetAddress(emailAddress));
		MailServiceUtil.sendEmail(mailMessage);


		return user;
	}

	protected Credential exchangeCode(
			long companyId, String authorizationCode, String redirectUri)
		throws CodeExchangeException, SystemException {

		try {
			GoogleAuthorizationCodeFlow flow = getFlow(companyId);

			GoogleAuthorizationCodeTokenRequest token = flow.newTokenRequest(
				authorizationCode);

			token.setRedirectUri(redirectUri);

			GoogleTokenResponse response = token.execute();

			/*GoogleCredential gc = new GoogleCredential();
		    gc.setAccessToken(response.getAccessToken());

		    contactsService contactsService = new ContactsService("Lasso Project");
		    contactsService.setOAuth2Credentials(gc);

		    try {
		        URL feedUrl = new URL("https://www.google.com/m8/feeds/contacts/default/full");

		        Query myQuery = new Query(feedUrl);
		        myQuery.setMaxResults(1000);

		        ContactFeed resultFeed = contactsService.query(myQuery, ContactFeed.class);

		        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
		            out.println(resultFeed.getEntries().get(i).getTitle().getPlainText() + "<br/>");
		        }

		    } catch (Exception e) {
		        System.out.println(e);
		    }*/


			return flow.createAndStoreCredential(response, null);
		}
		catch (IOException e) {
			System.err.println("An error occurred: " + e);

			throw new CodeExchangeException();
		}
	}

	protected GoogleAuthorizationCodeFlow getFlow(long companyId)
		throws IOException, SystemException {

		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();

		GoogleAuthorizationCodeFlow.Builder builder = null;

		String googleClientId ="187858048344-qfg0q1dfjtm0974hi3ujrdo6ou85e9qs.apps.googleusercontent.com";
		String googleClientSecret = "PN4mVz7NfqobFzsQtz0sn_AQ";

		List<String> scopes = null;

		if (DeployManagerUtil.isDeployed(_GOOGLE_DRIVE_CONTEXT)) {
			scopes = _SCOPES_DRIVE;
		}
		else {
			scopes = _SCOPES_LOGIN;
		}

		if (Validator.isNull(googleClientId) ||
			Validator.isNull(googleClientSecret)) {

			InputStream is = GoogleOAuth.class.getResourceAsStream(
				_CLIENT_SECRETS_LOCATION);

			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				jsonFactory, new InputStreamReader(is));

			builder = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, scopes);
		}
		else {
			builder = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, googleClientId, googleClientSecret,
				scopes);
		}

		String accessType = "online";

		if (DeployManagerUtil.isDeployed(_GOOGLE_DRIVE_CONTEXT)) {
			accessType = "offline";
		}

		builder.setAccessType(accessType);
		builder.setApprovalPrompt("force");

		return builder.build();
	}

	protected Userinfo getUserInfo(Credential credentials)
		throws NoSuchUserIdException {

		Oauth2.Builder builder = new Oauth2.Builder(
			new NetHttpTransport(), new JacksonFactory(), credentials);

		Oauth2 oauth2 = builder.build();

		Userinfo userInfo = null;

		try {
			userInfo = oauth2.userinfo().get().execute();

		}
		catch (IOException e) {
			System.err.println("An error occurred: " + e);
		}

		if ((userInfo != null) && (userInfo.getId() != null)) {
			return userInfo;
		}
		else {
			throw new NoSuchUserIdException();
		}
	}

	protected void redirectUpdateAccount(
			HttpServletRequest request, HttpServletResponse response, User user)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		PortletURL portletURL = PortletURLFactoryUtil.create(
			request, PortletKeys.LOGIN, themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		portletURL.setParameter("saveLastPath", Boolean.FALSE.toString());
		portletURL.setParameter("struts_action", "/login/update_account");

		PortletURL redirectURL = PortletURLFactoryUtil.create(
			request, PortletKeys.FAST_LOGIN, themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		redirectURL.setParameter("struts_action", "/login/login_redirect");
		redirectURL.setParameter("emailAddress", user.getEmailAddress());
		redirectURL.setParameter("anonymousUser", Boolean.FALSE.toString());
		redirectURL.setPortletMode(PortletMode.VIEW);
		redirectURL.setWindowState(LiferayWindowState.POP_UP);

		portletURL.setParameter("redirect", redirectURL.toString());
		portletURL.setParameter("userId", String.valueOf(user.getUserId()));
		portletURL.setParameter("emailAddress", user.getEmailAddress());
		portletURL.setParameter("firstName", user.getFirstName());
		portletURL.setParameter("lastName", user.getLastName());
		portletURL.setPortletMode(PortletMode.VIEW);
		portletURL.setWindowState(LiferayWindowState.POP_UP);

		response.sendRedirect(portletURL.toString());
	}

	protected void sendLoginRedirect(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		PortletURL portletURL = PortletURLFactoryUtil.create(
			request, PortletKeys.FAST_LOGIN, themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		portletURL.setWindowState(LiferayWindowState.POP_UP);

		portletURL.setParameter("struts_action", "/login/login_redirect");

		response.sendRedirect(portletURL.toString());
	}

	protected User setGoogleCredentials(
			HttpSession session, long companyId, Credential credential)
		throws Exception {
System.out.println("--set google credential--");
		Userinfo userinfo = getUserInfo(credential);

		if (userinfo == null) {
			return null;
		}

		User user = null;

		String emailAddress = userinfo.getEmail();

		if ((user == null) && Validator.isNotNull(emailAddress)) {
			try{
			  user = UserLocalServiceUtil.getUserByEmailAddress(
				companyId, emailAddress);
		    }
		    catch(NoSuchUserException noSuchUserException){
		    	user=null;
		    }

			if ((user != null) &&
				(user.getStatus() != WorkflowConstants.STATUS_INCOMPLETE)) {

				session.setAttribute("GOOGLE_USER_EMAIL_ADDRESS", emailAddress);
			}
		}

		if (user != null) {
			if (user.getStatus() == WorkflowConstants.STATUS_INCOMPLETE) {
				session.setAttribute(
					"GOOGLE_INCOMPLETE_USER_ID", userinfo.getId());

				user.setEmailAddress(userinfo.getEmail());
				user.setFirstName(userinfo.getGivenName());
				user.setLastName(userinfo.getFamilyName());

				return user;
			}
			System.out.println("--Coming here--");
			user = updateUser(user, userinfo,companyId);
		}
		else {
			user = addUser(session, companyId, userinfo);
		}

		if (DeployManagerUtil.isDeployed(_GOOGLE_DRIVE_CONTEXT)) {
			updateCustomFields(
				user, userinfo, credential.getAccessToken(),
				credential.getRefreshToken());
		}

		return user;
	}

	protected void updateCustomFields(
			User user, Userinfo userinfo, String accessToken,
			String refreshToken)
		throws PortalException, SystemException {

		ExpandoValueLocalServiceUtil.addValue(
			user.getCompanyId(), User.class.getName(),
			ExpandoTableConstants.DEFAULT_TABLE_NAME, GOOGLE_ACCESS_TOKEN,
			user.getUserId(), accessToken);

		ExpandoValueLocalServiceUtil.addValue(
			user.getCompanyId(), User.class.getName(),
			ExpandoTableConstants.DEFAULT_TABLE_NAME, GOOGLE_REFRESH_TOKEN,
			user.getUserId(), refreshToken);

		ExpandoValueLocalServiceUtil.addValue(
			user.getCompanyId(), User.class.getName(),
			ExpandoTableConstants.DEFAULT_TABLE_NAME, GOOGLE_USER_ID,
			user.getUserId(), userinfo.getId());
	}

	protected User updateUser(User user, Userinfo userinfo,long companyId) throws Exception {
		String emailAddress = userinfo.getEmail();
		String firstName = userinfo.getGivenName();
		String lastName = userinfo.getFamilyName();
		boolean male = Validator.equals(userinfo.getGender(), "male");

		if (emailAddress.equals(user.getEmailAddress()) &&
			firstName.equals(user.getFirstName()) &&
			lastName.equals(user.getLastName()) && (male == user.isMale())) {


			List<UserGroupRole> userGroupRoles = null;

			Role role=null;
			Role socialUserRole=null;
			try {
				List<Role> list=RoleLocalServiceUtil.getRoles(0, RoleLocalServiceUtil.getRolesCount());
				for(Role role2:list){
					if(role2.getName().equalsIgnoreCase("Professional")){
						role=role2;
						break;
					}
				}
				list = RoleLocalServiceUtil.getRoles(
						0, RoleLocalServiceUtil.getRolesCount());
				for (Role role2 : list) {
					if (role2.getName().equalsIgnoreCase("Social Office User")) {
						socialUserRole = role2;

						break;
					}
				}
			}catch(Exception e)
			{
				 System.out.println("--ROLE EXCEPTION RAISED--");
			}

			//long userGroupIds[] = { userGroup.getUserGroupId() };
			long[] roleIds={role.getRoleId(),socialUserRole.getRoleId()};

			UserGroup userGroup = null;
			try {
				System.out.println("--Total UserGroup--"
						+ UserGroupLocalServiceUtil.getUserGroupsCount());
				List<UserGroup> list = UserGroupLocalServiceUtil.getUserGroups(
						0, RoleLocalServiceUtil.getRolesCount());
				for (UserGroup userGroup2 : list) {
					if (userGroup2.getName().equalsIgnoreCase("Professional")) {
						userGroup = userGroup2;

						break;
					}
				}

			} catch (Exception e) {
			   System.out.println("--USER GROUP EXCEPTION RAISED--");
			}

			long userGroupIds[] = {userGroup.getUserGroupId()};
			Contact contact = user.getContact();

			Calendar birthdayCal = CalendarFactoryUtil.getCalendar();

			birthdayCal.setTime(contact.getBirthday());

			int birthdayMonth = birthdayCal.get(Calendar.MONTH);
			int birthdayDay = birthdayCal.get(Calendar.DAY_OF_MONTH);
			int birthdayYear = birthdayCal.get(Calendar.YEAR);

			long[] groupIds = null;
			long[] organizationIds = null;
			ServiceContext serviceContext = new ServiceContext();
			String password=randomString(6);

			User checkUser=UserLocalServiceUtil.updateUser(
					user.getUserId(), StringPool.BLANK, password,
					password, false, user.getReminderQueryQuestion(),
					user.getReminderQueryAnswer(), user.getScreenName(), emailAddress,
					0, user.getOpenId(), user.getLanguageId(), user.getTimeZoneId(),
					user.getGreeting(), user.getComments(), firstName,
					user.getMiddleName(), lastName, contact.getPrefixId(),
					contact.getSuffixId(), male, birthdayMonth, birthdayDay,
					birthdayYear, contact.getSmsSn(), contact.getAimSn(),
					contact.getFacebookSn(), contact.getIcqSn(), contact.getJabberSn(),
					contact.getMsnSn(), contact.getMySpaceSn(), contact.getSkypeSn(),
					contact.getTwitterSn(), contact.getYmSn(), contact.getJobTitle(),
					groupIds, organizationIds, roleIds, userGroupRoles, userGroupIds,
					serviceContext);
			Group group= GroupLocalServiceUtil.getGroup(companyId, "Professional");
			Role roleInfo=RoleLocalServiceUtil.getRole(companyId, "Professional");
			long userIds[]={checkUser.getUserId()};


			System.out.println(group.getGroupId()+"---UPDATE USER---"+roleInfo.getRoleId());

			UserGroupRoleLocalServiceUtil.addUserGroupRoles(userIds, group.getGroupId(),roleInfo.getRoleId());
			MailMessage mailMessage=new MailMessage();
			mailMessage.setHTMLFormat(true);
			mailMessage.setBody("<div style='width:600px;height:650px;background-color: rgba(0, 32, 73, 0.95);'>"
                                 +"<div style='height:100px;border-bottom:2px solid #00948f'>" +
                                 "<img src='"+"././images/img1.png' style='width:100px;height:50px;' />"+"</div>"
                                 +"<div style='height:450px;padding:20px;color:white;'>"
                                 +"Hello <strong>"+firstName+" "+lastName+"</strong>,<br/><br/>"
	                             +"<span>Welcome to the jobengine.com. Thanks for updating your account with us!!!</span><br/>"
	                             +"<span>First thing first..</span><br/><br/>"
	                             +"<ul style='list-style-type:none;text-decoration:none;'>"
						         +"<li style='float:left;'><strong>Your UserName Is:</strong><li>"+emailAddress+"<br/>"
	                             +"<li style='float:left;'><strong>Your Password Is:</strong><li>"+password
	                             +"</ul> <br/><br/>"
	                             +"<span>To change your details, including your newsletter subscription and password simply "
	                             +"complete your registration process and login into the account.</span><br/><br/>"
	                             +"<strong>Let the fun begin!</strong><br/><br/>"
	                             +"<span>Get ready to have some serious fun with our fantastic range</span><br/><br/>"
	                             +"<strong> Good luck!<br/>JobEngine Support Team</strong><br/><br/>"
	                             +"<span style=''><u>http://localhost:8080</u></span><br/>"
								 +"<strong>Free Help & Support</strong><br/>"
								 +"<strong>Email:</strong><u>support@jobengine.in</u><br/>"
								 +"<strong><u>FAQ</u></strong>	<br/><br/><br/>"
								 +"<span>*Conditions Apply</span><br/><br/>"
								 +"<small>Users must be 18 or over to register this site."
								 +"Head Office: JobEngine India Network Ltd.1 35,2nd Floor, Near Railway Station, Jabalpur - 400 018</small> </div></div>");

			mailMessage.setFrom(new InternetAddress("vchandrol@gmail.com","Vikash Kumar"));
			mailMessage.setSubject("Thank you for your account updation");
			mailMessage.setTo(new InternetAddress(emailAddress));
			MailServiceUtil.sendEmail(mailMessage);
			return user;
		}

		Contact contact = user.getContact();

		Calendar birthdayCal = CalendarFactoryUtil.getCalendar();

		birthdayCal.setTime(contact.getBirthday());

		int birthdayMonth = birthdayCal.get(Calendar.MONTH);
		int birthdayDay = birthdayCal.get(Calendar.DAY_OF_MONTH);
		int birthdayYear = birthdayCal.get(Calendar.YEAR);

		long[] groupIds = null;
		long[] organizationIds = null;
		//long[] roleIds = null;
		List<UserGroupRole> userGroupRoles = null;

		Role role=null;
		try {
			List<Role> list=RoleLocalServiceUtil.getRoles(0, RoleLocalServiceUtil.getRolesCount());
			for(Role role2:list){
				if(role2.getName().equalsIgnoreCase("Professional")){
					role=role2;
					break;
				}
			}
		}catch(Exception e)
		{
			 System.out.println("--ROLE EXCEPTION RAISED--");
		}

		//long userGroupIds[] = { userGroup.getUserGroupId() };
		long[] roleIds={role.getRoleId()};

		UserGroup userGroup = null;
		try {
			System.out.println("--Total UserGroup--"
					+ UserGroupLocalServiceUtil.getUserGroupsCount());
			List<UserGroup> list = UserGroupLocalServiceUtil.getUserGroups(
					0, RoleLocalServiceUtil.getRolesCount());
			for (UserGroup userGroup2 : list) {
				if (userGroup2.getName().equalsIgnoreCase("Professional")) {
					userGroup = userGroup2;

					break;
				}
			}

		} catch (Exception e) {
		   System.out.println("--USER GROUP EXCEPTION RAISED--");
		}

		long userGroupIds[] = {userGroup.getUserGroupId()};

		ServiceContext serviceContext = new ServiceContext();

		/*if (!StringUtil.equalsIgnoreCase(
				emailAddress, user.getEmailAddress())) {*/
		if (emailAddress.equalsIgnoreCase(user.getEmailAddress())){}
		else {

			UserLocalServiceUtil.updateEmailAddress(
				user.getUserId(), StringPool.BLANK, emailAddress, emailAddress);
		}

		UserLocalServiceUtil.updateEmailAddressVerified(user.getUserId(), true);
		long userIds[]={user.getUserId()};
		Group group= GroupLocalServiceUtil.getGroup(companyId, "Professional");
		Role roleInfo=RoleLocalServiceUtil.getRole(companyId, "Professional");
		System.out.println(group.getGroupId()+"---UPDATE USER---"+roleInfo.getRoleId());

		UserGroupRoleLocalServiceUtil.addUserGroupRoles(userIds, group.getGroupId(),roleInfo.getRoleId());

		return UserLocalServiceUtil.updateUser(
			user.getUserId(), StringPool.BLANK, StringPool.BLANK,
			StringPool.BLANK, false, user.getReminderQueryQuestion(),
			user.getReminderQueryAnswer(), user.getScreenName(), emailAddress,
			0, user.getOpenId(), user.getLanguageId(), user.getTimeZoneId(),
			user.getGreeting(), user.getComments(), firstName,
			user.getMiddleName(), lastName, contact.getPrefixId(),
			contact.getSuffixId(), male, birthdayMonth, birthdayDay,
			birthdayYear, contact.getSmsSn(), contact.getAimSn(),
			contact.getFacebookSn(), contact.getIcqSn(), contact.getJabberSn(),
			contact.getMsnSn(), contact.getMySpaceSn(), contact.getSkypeSn(),
			contact.getTwitterSn(), contact.getYmSn(), contact.getJobTitle(),
			groupIds, organizationIds, roleIds, userGroupRoles, userGroupIds,
			serviceContext);
	}
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random rnd = new Random();

	String randomString( int len )
	{
	   StringBuilder sb = new StringBuilder( len );
	   for( int i = 0; i < len; i++ ) {
		sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	}
	   return sb.toString();
	}
	private static final String _CLIENT_SECRETS_LOCATION =
		"client_secrets.json";

	private static final String _GOOGLE_DRIVE_CONTEXT = "google-drive-hook";



	private static final List<String> _SCOPES_DRIVE = Arrays.asList(
		"https://www.googleapis.com/auth/userinfo.email",
		"https://www.googleapis.com/auth/userinfo.profile",
		"https://www.googleapis.com/auth/drive");

	private static final List<String> _SCOPES_LOGIN = Arrays.asList(
		"https://www.googleapis.com/auth/userinfo.email",
		"https://www.googleapis.com/auth/userinfo.profile");
}
