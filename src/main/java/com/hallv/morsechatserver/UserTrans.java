/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hallv.morsechatserver;

import java.io.Serializable;

/**
 * Container class to transfer information about other users
 * down to the client.
 * @author hallv
 */
public class UserTrans implements Serializable {
    public String username;
    public long id;
    
    public UserTrans(String username, long id){
        this.username = username;
        this.id = id;
    }
    
    public UserTrans(){
        
    }
}
