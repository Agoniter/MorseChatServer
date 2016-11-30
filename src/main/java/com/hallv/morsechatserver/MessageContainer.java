package com.hallv.morsechatserver;

import java.io.Serializable;
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Vegard
 */
public class MessageContainer implements Serializable{
    ArrayList<Long> recipients;
    ArrayList<Long> message;
    Long sender;
    
    public MessageContainer(){    
    }

    public ArrayList<Long> getRecipients() {
        return recipients;
    }

    public void setRecipients(ArrayList<Long> recipients) {
        this.recipients = recipients;
    }

    public ArrayList<Long> getMessage() {
        return message;
    }

    public void setMessage(ArrayList<Long> message) {
        this.message = message;
    }

    public Long getSender() {
        return sender;
    }

    public void setSender(Long sender) {
        this.sender = sender;
    }
    
    
    
    
}
