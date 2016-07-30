package com.action.login.events;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*import com.action.login.landingpage.LandingPageType;
import com.action.login.landingpage.LandingPageTypeFactory;*/
import com.action.login.util.CustomLandingPageConstant;
import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.struts.LastPath;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

public class CustomLandingPageAction extends Action {

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response)
			throws ActionException {



		try
		{
			doRun(request, response);
		} catch (Exception e)
		{
			throw new ActionException(e);
		}
	}

	protected void doRun(final HttpServletRequest request, final HttpServletResponse response)
			throws SystemException, PortalException,Exception
	{
		long companyId = PortalUtil.getCompanyId(request);

		String path;

			path = PrefsPropsUtil.getString(companyId, PropsKeys.DEFAULT_LANDING_PAGE_PATH);

			// Check for override.default.landing.page.path property value

			LOG.info(PropsKeys.DEFAULT_LANDING_PAGE_PATH + StringPool.EQUAL + path);


			boolean overrideDefaultLandingPagePath = GetterUtil.getBoolean(PrefsPropsUtil.getString(companyId,
					CustomLandingPageConstant.OVERRIDE_DEFAULT_LANDING_PAGE_PATH));

			LOG.info(CustomLandingPageConstant.OVERRIDE_DEFAULT_LANDING_PAGE_PATH
									+ StringPool.EQUAL + overrideDefaultLandingPagePath);

			if ((Validator.isNull(overrideDefaultLandingPagePath) || !overrideDefaultLandingPagePath)
					&& LOG.isInfoEnabled())
			{
				LOG.info("Please set 'override.default.landing.page.path=true' "
						+ "in hook's portal.properties to enable user to land on custom landing page using "
						+ "Custom Landing Page Hook");
			}

			if (overrideDefaultLandingPagePath)
			{
				path = getCustomLandingPage(request);


					LOG.info("Custom Landing Page path" + StringPool.EQUAL + path + " for User : "
							+ PortalUtil.getUser(request).getFullName());

			}
			else if (Validator.isNotNull(path))
			{
				if (path.contains("${liferay:screenName}") || path.contains("${liferay:userId}"))
				{
					User user = PortalUtil.getUser(request);
					if (Validator.isNotNull(user))
					{
						path = StringUtil.replace(
								path,
								new String[] { "${liferay:screenName}", "${liferay:userId}" },
								new String[] { HtmlUtil.escapeURL(user.getScreenName()),
										String.valueOf(user.getUserId()) });
					}
				}
			}

			if (Validator.isNotNull(path))
			{
				HttpSession session = request.getSession();
				session.setAttribute(WebKeys.LAST_PATH, new LastPath(StringPool.BLANK, path));
			}


	}
	private String getCustomLandingPage(final HttpServletRequest request) throws PortalException,
	SystemException
	{
		String customLandingPagePath="";
		try {
			User user=PortalUtil.getUser(request);

			List<Role> roles=user.getRoles();
			for(Role role:roles){
				if(role.getName().equalsIgnoreCase("Administrator")){
					customLandingPagePath="/group/administrator/dashboard";
					break;
				}
				else if(role.getName().equalsIgnoreCase("Employee")){
					customLandingPagePath="/group/employee/dashboard";
					break;
				}
				else if(role.getName().equalsIgnoreCase("Manager")){
					customLandingPagePath="/group/manager/dashboard";
					break;
				}
				else if(role.getName().equalsIgnoreCase("Support")){
					customLandingPagePath="/group/support/dashboard";
				}
			}





		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
			return customLandingPagePath;
	}

	private static final Log LOG = LogFactory.getLog(CustomLandingPageAction.class);
}