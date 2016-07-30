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

package com.liferay.google;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.BaseAutoLogin;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
* @author Sergio González
*/
public class GoogleAutoLogin extends BaseAutoLogin {

@Override
protected String[] doLogin(
		HttpServletRequest request, HttpServletResponse response)
	throws Exception {

	long companyId = PortalUtil.getCompanyId(request);

	boolean googleAuthEnabled = true;

	if (!googleAuthEnabled) {
		return null;
	}

	User user = getUser(request, companyId);

	if (user == null) {
		return null;
	}

	String[] credentials = new String[3];

	credentials[0] = String.valueOf(user.getUserId());
	credentials[1] = user.getPassword();
	credentials[2] = Boolean.TRUE.toString();

	return credentials;
}

protected User getUser(HttpServletRequest request, long companyId)
	throws PortalException, SystemException {

	HttpSession session = request.getSession();

	String emailAddress = GetterUtil.getString(
		session.getAttribute("GOOGLE_USER_EMAIL_ADDRESS"));

	if (Validator.isNull(emailAddress)) {
		return null;
	}

	session.removeAttribute("GOOGLE_USER_EMAIL_ADDRESS");

	User user = UserLocalServiceUtil.getUserByEmailAddress(
		companyId, emailAddress);

	return user;
}

}