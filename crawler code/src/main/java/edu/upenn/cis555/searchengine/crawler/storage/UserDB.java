// package edu.upenn.cis455.storage;

// import java.util.ArrayList;


// import com.sleepycat.je.Transaction;
// import com.sleepycat.persist.EntityCursor;
// import com.sleepycat.persist.EntityStore;
// import com.sleepycat.persist.PrimaryIndex;

// public class UserDB {
	
// 	private PrimaryIndex<String,User> useByUser;

// 	public UserDB(EntityStore store) {
// 		useByUser = store.getPrimaryIndex(String.class, User.class);
// 	}
	
// 	public User get(String userName) {
// 		return useByUser.get(userName);
// 	}
	
// 	public ArrayList<User> getallUsers() {
// 		ArrayList<User> users;
// 		EntityCursor<User> iterUsers = useByUser.entities();
// 		try {
// 			users = new ArrayList<User>();
// 			for (User d : iterUsers) {
// 				users.add(d);
// 			}
// 		} finally {
// 			iterUsers.close();
// 		}
// 		return users;
// 	}
	
// 	public boolean insertUser(User user, Transaction txn) {
// 		return useByUser.putNoOverwrite(txn, user);
//     }
    
// 	public void updateUser(User user, Transaction txn) {
// 		useByUser.put(txn, user);
// 	}
// }
