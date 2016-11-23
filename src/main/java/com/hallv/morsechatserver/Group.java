/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hallv.morsechatserver;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author Vegard
 */
@Entity
@Table(name = "usergroups")
public class Group implements Serializable {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    
    @Id String groupname;
    @Id String username;
    
    
    public Group(){
    }
    
    public Group(String name, String username){
        this.groupname = name;
        this.username = username;
    }

}
