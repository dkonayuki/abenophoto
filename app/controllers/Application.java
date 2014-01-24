package controllers;


import play.*;
import play.mvc.*;
import sun.misc.BASE64Encoder;

import java.io.File;


import models.*;

import javax.persistence.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import models.User;
import play.mvc.Controller;
import play.mvc.Http;



public class Application extends Controller {

	public static void index() {
		User user = getCurrentUser();
		
		if (user != null)
			home();
		else
			login_signup();
	}    

	public static void login_signup(){render();}

	public static void post_login_info() throws NoSuchAlgorithmException{
		// 標準出力に入力されたパラメータの名前を出力
		System.out.println(params.all().keySet());
		String name =params.get("name");
		String pass =params.get("pass");
		System.out.println(name+" "+pass);
		Map<String, Object> map = new HashMap<String, Object>();
		//name,passあってるかの確認
		List<User> users =User.find("username = ?",	name).fetch();
		if(users.size()!=1){//名前が存在しないor同名が存在（ありえないが念のため）
			map.put("result", "Error: no such name");
		}else{
			if(users.get(0).compare_pass(pass)){
				map.put("result", "OK");
				session.put("login_user", users.get(0).id);
			}else{
				map.put("result", "Error: wrong password");
			}
		}
		// Map に結果を蓄え，JSON として出力
		renderJSON(map);
	}

	public static void post_signup_info() throws NoSuchAlgorithmException{
		// 標準出力に入力されたパラメータの名前を出力
		System.out.println(params.all().keySet());
		String name =params.get("name");
		String pass =params.get("pass");
		System.out.println(name+" "+pass);
		Map<String, Object> map = new HashMap<String, Object>();

		//名前かぶってないか確認
		List<User> users =User.find("username = ?",	name).fetch();	
		if(!users.isEmpty()){//名前被り処理
			map.put("result", "Error: the name is already exist");
		}else{
			map.put("result", "OK");
			// アンケートデータをデータベースに書き込む
			User user = new User(name, pass);
			user.save();

			
			// 埋め込むべき変数をテンプレートに渡す．
			// テンプレートからはキー名で参照できる．
			// テンプレートからは，${entry.id} として該当データで自動生成された
			// ID番号を取得している． 
			renderArgs.put("user", user);

			session.put("login_user", user.id);
		}
		// Map に結果を蓄え，JSON として出力
		renderJSON(map);
	}

	public static void profile(){
		User user = getCurrentUser();
		//File
		render(user);
	}

	public static void postEditProfile(File uploadAvatar){
		Long id = Long.parseLong(session.get("login_user"));
		User user = User.find("id = ?", id).first();
		String nickname = params.get("nickname");
		String profile  = params.get("profile");
		user.set_nickname(nickname);
		user.set_profile(profile);
		user.save();
		if(uploadAvatar != null){
			Avatar.create(uploadAvatar, id);
		}    	
		profile();
	}    	

	/*
	 * avatar content
	 */
	public static void avatarContent(String name) {
		Avatar avatar = Avatar.findByName(name);
		renderBinary(avatar.file);
	}

	public static void toppage() {
		User user = getCurrentUser();
		if (user == null)
			login_signup();
		else
			render(user);
	}

	public static User getCurrentUser() {
		String userId = session.get("login_user");
		if (userId == null){
			login_signup();
			return null;
		}
		Long id = Long.parseLong(session.get("login_user"));
		User user = User.find("id = ?", id).first();
		if(user.get_avatar() == null || user.get_avatar().equals("")){
			user.set_avatar("/public/images/default.png");
			user.save();
		}
		return user;
	}

	public static void home() {
		User user = getCurrentUser();
		if (user == null)
			login_signup();
		else{	
			List<Photo> photos = Photo.find("user = ?", user).fetch();
			render(photos, user);
		}
	}

	public static void upload(String title, String tags, String caption, File image) {
		if (image != null) {
			User user = getCurrentUser();
			String targetPath = "public/uploads/" + user.get_username().toString() + "/photos";
			File dir = new File(targetPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			targetPath += "/" + image.getName();
			image.renameTo(new File(targetPath));
			Photo photo = new Photo(targetPath, title, caption, user);
			photo.save();
			user.addPhoto(photo);
			user.save();
			System.out.println("File saved in " + targetPath);
			
			//tags
			//spaceでsplit
			String[] tagNames = tags.replaceAll("　", " ").split(" ");
			for(String tagName : tagNames){
				//重複確認
				Tag tag = Tag.find("name = ?", tagName).first();
				if(tag == null){
					tag = new Tag(tagName);
					tag.save();
				}
				//タグと写真の関係を保存
				TagPhotoRelation tpr = new TagPhotoRelation(tag, photo);
				tpr.save();
			}
		} else {
			System.out.println("File not found");
		}  
		home();
	}
	
	public static void serchResult(){
		User user = getCurrentUser();
		
		String serch = params.get("serch");
		serch = "%" + serch +"%";
		
		//List<User> results = User.find("username like ? OR nickname like ?", serch, serch).fetch();
		//render(user, results);
		

		List<Photo> photos = new ArrayList();
		List<Tag> tags = Tag.find("name like ?", serch).fetch();
		for (Tag tag : tags){
			List<TagPhotoRelation> relations = TagPhotoRelation.find("tagId = ?", tag.id).fetch();
					for(TagPhotoRelation relation : relations){
						Photo photo = Photo.find("id = ?", relation.get_photoId()).first();
						System.out.println("写真" +photo.id);
						if(photo != null)
							photos.add(photo);
					}
		}
		
		System.out.println(photos);
		render(user, photos);
	}

	public static void logout(){
		session.clear();
		login_signup();
	}
	

	public static void photoviewer(String url) {
		/* 
		 * get photo from parent
		 */
		
		User user = getCurrentUser();
		Photo photo = Photo.find("url = ?", url).first();
		
		List<Comment> comments = Comment.find("photo = ?", photo).fetch();
		
		/*
		if (comments.size() < 1) {
			photo.addComment(new Comment(user, "this is a default comment", photo));
			comments = photo.getComment();
		}
		*/
			
		// render
		if (user == null)
			render(photo, comments);
		else
			render(photo, comments, user);
	}
	
	public static void user(long id) {
		User user = User.find("id = ?", id).first();
		if (user == null)
			home();
		else {
				List<Photo> photos = Photo.find("user = ?", user).fetch();
				render(photos, user);
			}
		}

	public static void follower(){
		User user = getCurrentUser();
		//List<User> follower = user.get_folower();
		render(user);
	}
	
	public static void follow(long id) {
		User currentUser = getCurrentUser();
		User user = User.find("id = ?", id).first();
		//add follower
		FollowingData data = new FollowingData(currentUser, user);
		data.save();

		System.out.println("**********************");
		System.out.println("user:" + user.get_username());
		System.out.println("currentuser:" + currentUser.get_username());
		System.out.println(user.get_username() + "'s followers count:" + user.getFollowerCount());
		System.out.println("Follower:" + data.getFollower().get_username());
		System.out.println(currentUser.get_username() + "'s followings count:" + currentUser.getFollowingCount());
		System.out.println("Following:" + data.getFollowee().get_username());

		if (user.isFollowed(currentUser.id)) {
			System.out.println(user.get_username() + " is followed by " + currentUser.get_username());
		} else {
			System.out.println("wrooooooong");
		}
		user(id);
	}
	
	public static void unFollow(long id) {
		User currentUser = getCurrentUser();
		User user = User.find("id = ?", id).first();

		FollowingData data = FollowingData.find("follower = ? AND followee = ?", currentUser, user).first();
		data.delete();

		System.out.println("**********************");
		System.out.println("user:" + user.get_username());
		System.out.println("currentuser:" + currentUser.get_username());
		System.out.println( user.get_username() + "'s followers count:" + user.getFollowerCount());
		System.out.println(currentUser.get_username() + "'s followings count:" + user.getFollowingCount());

		if (user.isFollowed(currentUser.id)) {
			System.out.println("wrooooooong");
		} else {
			System.out.println(user.get_username() + " is not followed by " + currentUser.get_username());
		}
		user(id);
	}
}
