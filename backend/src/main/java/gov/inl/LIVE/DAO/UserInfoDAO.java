/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.inl.LIVE.DAO;

import org.springframework.stereotype.Service;

/**
 *
 * @author monejh
 */
@Service("userInfoDAO")
public class UserInfoDAO extends UserInfoRepository
{
    
    protected UserInfoDAO()
    {
        super();
    }
    
}
