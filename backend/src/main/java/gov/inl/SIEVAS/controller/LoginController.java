/* 
 * Copyright 2017 Idaho National Laboratory.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.inl.SIEVAS.controller;

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
