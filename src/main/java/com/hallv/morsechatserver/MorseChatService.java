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
    
    /**
     * Basic test method to check if service is up and running
     * @return  Response code 200 and "Hello World!"
     */
    @GET
    @Path("test")
    public Response test(){
        return Response.ok("Hello World!").build();
    }
    
    /**
     * Checks a users token, used for debugging
     * @return  Response code 200 OK if authenticated, 401 Unauthorized if not.
     */
    @GET
    @Secured
    @Path("debug/checktoken")
    public Response dbgToken(){
        return Response.ok("User is authenticated").build();
    }
    
    /**
     * Checks if a given user is in the database, used for debugging
     * @param user  The username to check for in the database
     * @return      Response code 200 OK, the string User not found if not found, the username if found.
     */
    @GET
    @Path("debug/checkuser")
    public Response checkUsr(@QueryParam("user") String user){
        List<ChatUser> results = em.createQuery("select c from ChatUser c where c.username=:name").setParameter("name", user).getResultList();
        
        if(!results.isEmpty()){
            return Response.ok(results.get(0).getUsername() + " - " + results.get(0).getPassword()).build(); 
        }
        return Response.ok("User not found").build();
    }
    
    /**
     * Adds a test message to a given user, from the specified sender. Used for debugging.
     * @param sender    The username of the user that the message should be sent from.
     * @param recipient The username of the user that should receive the message.
     * @return          Response code 200 OK if successful at sending message, 503 Service Unavailable if something went wrong.
     */
    @GET
    @Path("debug/fillmessages")
    public Response fillMsgs(@QueryParam("sender") String sender,
                             @QueryParam("recipient") String recipient){
       
        List<ChatUser> senders = em.createQuery("select c from ChatUser c where c.username=:name").setParameter("name", sender).getResultList();
        List<ChatUser> recipients = em.createQuery("select c from ChatUser c where c.username=:name").setParameter("name", recipient).getResultList();
        
        if(senders.isEmpty() || recipients.isEmpty()){
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
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
    
    /**
     * Lets an authenticated user send a message.
     * @param msgCont   An object with the message along with the recipients of the message.
     * @see             MessageContainer
     * @return          Response code 200 OK if successful at sending message, 503 Service Unavailable if something went wrong.
     */
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

    /**
     * Gets all the messages of an authenticated user
     * @param recipientid   The userID of the user who's messages should be retrieved.
     * @return              The messages of the user.
     */
    @GET
    @Secured
    @Path("message/messages")
    public List<Message> getMessages(@QueryParam("recipientid") long recipientid){
        return em.createQuery("select m from Message m where m.recipient.id = :recipientid",Message.class)
                  .setParameter("recipientid",recipientid)
                  .getResultList();

    }
    
    /**
     * Gets all users in the database except for one (usually the logged in user)
     * @param userid    The userID that shouldn't be returned
     * @see             UserTrans
     * @return          All users except the one specified.
     */
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
    
    /**
     * Deletes the specified message
     * @param messageid The ID of the message that should be deleted
     * @return          Response code 200 OK if successful, 503 Service Unavailable if failed.
     */
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
    
    /**
     * Creates a friendship between the two users specified
     * @param ownerid   The ID of the user initiating the friendship
     * @param friendid  The ID of the user that is being befriended
     * @return          Response code 200 OK if successful, 503 Service Unavailable if failed.
     */
    @POST
    @Secured
    @Path("friend/add")
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
    @Path("friend/confirm")
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
    
    /**
     * Gets all the friends of a specified user
     * @param ownerid   The user who's friends should be retrieved.
     * @return          The friends.
     */
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
    
    /**
     * Searches the database for a username that has a given string in it
     * @param searchString  The string to search for.
     * @see                 UserTrans
     * @return              The users that match the search string
     */
    @GET
    @Secured
    @Path("user/search")
    public List<UserTrans> userSearch(@QueryParam("searchstring") String searchString){
        List<ChatUser> userSearch = em.createQuery("Select c from ChatUser c where c.username like :searchString").setParameter("searchString", searchString)
                .getResultList();
        
                List<UserTrans> usrs = new ArrayList<>();
                for(ChatUser usr : userSearch){
                    usrs.add(new UserTrans(usr.getUsername(),usr.getId()));
                }
                return usrs;
    }

    /**
     * Creates/registers a new user
     * @param email     The email of the user
     * @param password  The password (plaintext) of the user
     * @param username  The username of the user
     * @return          Response code 200 OK if successful, 503 Service Unavailable if failed.
     */
    @POST
    @Path("user/create")
    public Response createUser(@FormParam("email")String email,
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
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        
        return Response.ok().build();
    }
    
    
    /**
     * Logs a given user in
     * @param username  The users username.
     * @param password  The users password (plaintext).
     * @return          The user information from the database, including a JWT (token) if successful. Response code 401 Unauthorized if login failed.
     */
    @POST
    @Path("user/login")
    @Produces("application/json")
    public Response authenticateUser(@FormParam("username") String username,
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
            return Response.ok(tmp).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build(); 
    }
    
    /**
     * Creates a token for a user that expires in one week
     * @param username  The username of the user
     * @return          String representation of the token.
     */
    private String issueToken(String username) {
        return createJWT(username, 7*24*60*60*1000);
    }
    
    /**
     * Generates the JWT (token)
     * @param subject   The username of the user the token is for.
     * @param ttlMillis Time to live in milliseconds
     * @return          The token as a string.
     */
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
