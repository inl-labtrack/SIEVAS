/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.inl.LIVE.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Handles the login. Spring Security handles the rest of it.
 * @author monejh
 */

@Controller
public class LoginController
{
    /***
     * Handles getting the login page.
     * @return 
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String getLogin()
    {
        return "login";
    }
    
    /***
     * Handles the logout process. Invalidates login and returns to login page.
     * @param request The request object.
     * @param response The response object.
     * @return The page to go to when done, which is login with an error 
     *          parameter
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String getLogout(HttpServletRequest request, HttpServletResponse response)
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null)
            new SecurityContextLogoutHandler().logout(request, response, auth);
        
        return "redirect:login?logout=true";
    }
}