package com.hallv.morsechatserver;


import com.hallv.morsechatserver.ChatUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hallv
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Table(name = "Message")
public class Message implements Serializable {
    @Id @GeneratedValue
    Long id;
    String message;
    
    
    @Temporal(javax.persistence.TemporalType.DATE)
    Date messageTimestamp = new Date();
    
    @XmlJavaTypeAdapter(ChatUser.UserAdapter.class)
    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    ChatUser recipient;
    
    @XmlJavaTypeAdapter(ChatUser.UserAdapter.class)
    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    ChatUser sender;
    public Message(){
    }
    public Message(String message, ChatUser recipient, ChatUser sender){
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return messageTimestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.messageTimestamp = timestamp;
    }
    
}
