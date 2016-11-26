package com.hallv.morsechatserver;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author hallv
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Table(name = "Friend")
public class Friend implements Serializable {
    @Id @GeneratedValue
    Long id;
   
    
    int confirmed;
    
    @XmlJavaTypeAdapter(ChatUser.UserAdapter.class)
    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    ChatUser owner;
    @XmlJavaTypeAdapter(ChatUser.UserAdapter.class)
    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    ChatUser friend;
    
    public Friend(){}
    
    public Friend(ChatUser owner, int confirmed, ChatUser friend){
        this.owner = owner;
        this.confirmed = confirmed;
        this.friend = friend;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatUser getOwner() {
        return owner;
    }

    public void setOwner(ChatUser owner) {
        this.owner = owner;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(int confirmed) {
        this.confirmed = confirmed;
    }

    public ChatUser getFriend() {
        return friend;
    }

    public void setFriend(ChatUser friend) {
        this.friend = friend;
    }
    
    
}
