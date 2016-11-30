package com.hallv.morsechatserver;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author hallv
 */
@Path("morsechat")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class MorseChatService {
    @PersistenceContext
    EntityManager em;
    
    @Resource(mappedName ="jdbc/MorseChat")
    
    @GET
    @Path("test")
    public Response test(){
        return Response.ok("Hello World!").build();
    }
    
    @GET
    @Path("debug/createuser")
    public Response dbgCrtUsr(){
        ChatUser tmp = new ChatUser("hello", "plz", "work");
        em.persist(tmp);
        return Response.ok("Good shiet").build();
    }
    
    @GET
    @Secured
    @Path("debug/checktoken")
    public Response dbgToken(){
        return Response.ok("User is authenticated").build();
    }
    
    @GET
    @Path("debug/checkuser")
    public Response checkUsr(@QueryParam("user") String user){
        List<ChatUser> results = em.createQuery("select c from ChatUser c where c.username=:name").setParameter("name", user).getResultList();
        
        if(!results.isEmpty()){
            return Response.ok(results.get(0).getUsername() + " - " + results.get(0).getPassword()).build(); 
        }
        return Response.ok("User not found").build();
    }
    
    @GET
    @Path("debug/cleanup")
    public Response cleanup(){
        List<ChatUser> result = em.createQuery("select c from ChatUser c where password=:empty").setParameter("empty","").getResultList();
        int res = em.createQuery("delete from ChatUser where password=:empty").setParameter("empty", "").executeUpdate();
        int res2 = em.createQuery("delete from Message where sender=recipient").executeUpdate();
        int res3 = 0;
        for(ChatUser cUser : result){
            res3 += em.createQuery("delete from Message where sender.id=:sndr or recipient.id=:recip").setParameter("sndr", cUser.getId()).setParameter("recip", cUser.getId()).executeUpdate();
        }
        return Response.ok("Successfully removed " + res + " users without a password, " + res2 + " messages with sender as recipient, and " + res3 + " messages belonging to the users without passwords").build();
    }
    
    @GET
    @Path("debug/fillmessages")
    public Response fillMsgs(@QueryParam("sender") String sender,
                             @QueryParam("recipient") String recipient){
       
        List<ChatUser> senders = em.createQuery("select c from ChatUser c where c.username=:name").setParameter("name", sender).getResultList();
        List<ChatUser> recipients = em.createQuery("select c from ChatUser c where c.username=:name").setParameter("name", recipient).getResultList();
        
        if(senders.isEmpty() || recipients.isEmpty()){
            return Response.ok("Recipient or sender not found").build();
        }
        ChatUser thisSender = senders.get(0);
        ChatUser thisRecipient = recipients.get(0);
        
        ArrayList<Long> msg = new ArrayList<>();
        msg.add(200L);
        msg.add(100L);
        msg.add(100L);
        msg.add(100L);
        msg.add(200L);
        
        Message message = new Message(msg, thisRecipient, thisSender);
        em.persist(message);
        
        return Response.ok().build();
    }
    
    @POST
    @Secured
    @Path("message/sendmessage")
    @Consumes("application/json")
    public Response sendGroupMessage(MessageContainer msgCont){
                            Long senderid = msgCont.getSender();
                            ArrayList<Long> recipients = msgCont.getRecipients();
                            ArrayList<Long> message = msgCont.getMessage();
                            ChatUser sender = em.getReference(ChatUser.class, senderid);
                            if(sender == null || recipients.isEmpty()){
                                return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                            }
                            for(long i : recipients){
                                ChatUser recipient = em.getReference(ChatUser.class, i);
                                Message msg = new Message(message, recipient,sender);
                                em.persist(msg);
                            }
                            return Response.ok().build();
    }

    @GET
    @Secured
    @Path("message/messages")
    public List<Message> getMessages(@QueryParam("recipientid") long recipientid){
        return em.createQuery("select m from Message m where m.recipient.id = :recipientid",Message.class)
                  .setParameter("recipientid",recipientid)
                  .getResultList();

    }
    
    @GET
    @Secured
    @Produces("application/json")
    @Path("user/all")
    public List<UserTrans> getUsers(@QueryParam("userid") long userid){
        List<ChatUser> users = em.createQuery("select c from ChatUser c where c.id != :userid")
                .setParameter("userid", userid).getResultList();
        
        List<UserTrans> userTmp = new ArrayList<>();
        for(ChatUser usr : users){
            userTmp.add(new UserTrans(usr.getUsername(),usr.getId()));
        }
        Logger.getLogger(MorseChatService.class.getName()).log(Level.SEVERE, "USERALL" + userTmp.get(0).username);

        return userTmp;
    }
    
    @DELETE
    @Secured
    @Path("message/delete")
    public Response deleteMessage(@FormParam("messageid") long messageid){
        Message msg = em.getReference(Message.class,messageid);
        if(msg != null){
            em.remove(msg);
            return Response.ok().build();
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
    
    @POST
    @Secured
    @Path("user/add")
    public Response addFriend(@FormParam("ownerid") long ownerid,
                          @FormParam("friendid") long friendid  ){
                ChatUser owner = em.getReference(ChatUser.class, ownerid);
                ChatUser friend = em.getReference(ChatUser.class, friendid);
                if(owner != null && friend != null){ 
                Friend newFriend = new Friend(owner, 1, friend);
                em.persist(newFriend);
                return Response.ok().build();
                }
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
    /*            
    @POST
    @Secured
    @Path("user/confirm")
    public Response confirmFriend(@FormParam("ownerid") long ownerid,
                              @FormParam("friendid") long friendid){
                List<Friend> list= em.createQuery("select f from Friend f where f.owner.id = :ownerid and f.friend.id = :friendid",Friend.class)
                        .setParameter("ownerid",ownerid).setParameter("friendid",friendid).getResultList();
                 if(!list.isEmpty()){
                     Friend friend = list.get(0);
                     friend.setConfirmed(1);
                     em.persist(friend);
                     return Response.ok().build();
                 }
                 else{
                 return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                 }
    }*/
    @GET
    @Secured
    @Path("user/friends")
    public List<ChatUser> showFriends(@QueryParam("ownerid") long ownerid){
            List<Friend> friendZone = em.createQuery("Select f from Friend f where f.owner.id = :ownerid", Friend.class)
                    .setParameter("ownerid",ownerid).getResultList();
            ArrayList<ChatUser> friendList = new ArrayList<>();
            for(Friend f : friendZone){
                ChatUser temp = f.getFriend();
                temp.clearConfInfo();
                friendList.add(temp);
            }
       return friendList;
    }
    @GET
    @Secured
    @Path("user/search")
    public List<ChatUser> userSearch(@QueryParam("searchstring") String searchString){
        List<ChatUser> userSearch = em.createQuery("Select c from ChatUser c where c.username like :searchString").setParameter("searchString", searchString)
                .getResultList();
                for(ChatUser usr : userSearch){
                    usr.clearConfInfo();
                }
                return userSearch;
    }

    
    @POST
    @Path("user/create")
    public String createUser(@FormParam("email")String email,
                             @FormParam("password")String password,
                             @FormParam("username") String username) {
        ChatUser user = null;
        try {
            byte[] pass = password.getBytes("UTF-8");
            
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(pass);
            user = new ChatUser(username, email, Base64.getEncoder().encodeToString(hash));
            em.persist(user);
        } catch(Exception e) {
            Logger.getLogger(MorseChatService.class.getName()).log(Level.SEVERE, "Failed to add user",e);
        }
        
        return user != null ? user.toString() : "Error"; 
    }
    
    @POST
    @Path("user/login")
    @Produces("application/json")
    public ChatUser authenticateUser(@FormParam("username") String username,
                                     @FormParam("password") String password) {

        try{
            password = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(password.getBytes()));
        }
        catch(Exception e){
            Logger.getLogger(MorseChatService.class.getName()).log(Level.SEVERE, "Failed to log in",e);
        }
        Logger.getLogger(MorseChatService.class.getName()).log(Level.INFO, username + " - " + password);
        List<ChatUser> result = em.createQuery("select c from ChatUser c where c.username = :uname and c.password = :psw").setParameter("uname", username).setParameter("psw",password).getResultList();
        
        // Authenticate the user, issue a token and return a response
        if(!result.isEmpty()){
            ChatUser tmp = result.get(0);
            String token = issueToken(username);
            tmp.setToken(token);
            return tmp;
        }
        ChatUser error = new ChatUser("error", "error", "error");
        return error; 
    }
    
    private String issueToken(String username) {
        return createJWT(username, 7*24*60*60*1000);
    }
    
    private String createJWT(String subject, long ttlMillis) {

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary("test");
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setIssuedAt(now)
                                    .setSubject(subject)
                                    .signWith(signatureAlgorithm, signingKey);

        //if it has been specified, let's add the expiration
        if (ttlMillis >= 0) {
        long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();

    }
}
