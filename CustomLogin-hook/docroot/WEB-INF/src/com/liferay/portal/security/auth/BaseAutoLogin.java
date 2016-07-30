package com.liferay.portal.security.auth;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseAutoLogin implements AutoLogin {

	public String[] handleException(
            HttpServletRequest request, HttpServletResponse response,
            Exception e)
        throws AutoLoginException {

        return doHandleException(request, response, e);
    }

    public String[] login(
            HttpServletRequest request, HttpServletResponse response)
        throws AutoLoginException {

        try {
            return doLogin(request, response);
        }
        catch (Exception e) {
            return handleException(request, response, e);
        }
    }

    protected String[] doHandleException(
            HttpServletRequest request, HttpServletResponse response,
            Exception e)
        throws AutoLoginException {

        if (request.getAttribute(AutoLogin.AUTO_LOGIN_REDIRECT) == null) {
            throw new AutoLoginException(e);
        }

        _log.error(e, e);

        return null;
    }

    protected abstract String[] doLogin(
            HttpServletRequest request, HttpServletResponse response)
        throws Exception;

    private static Log _log = LogFactoryUtil.getLog(BaseAutoLogin.class);

}
