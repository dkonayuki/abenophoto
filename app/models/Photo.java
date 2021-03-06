package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

import java.util.*;

@Entity
public class Photo extends Model{
    //public static int id; //	auto make, auto increment

	private String url;
	private String title;
	private String caption;
	//private HashMap tag;
	@ManyToOne
    private User user;
	@OneToMany(mappedBy="photo", cascade=CascadeType.ALL)
	private List<Comment> comments;
    private Date date;
    private HashMap<Long,Integer> rank;//<userid,point>
    
    public void put_rank(long user_id,int point){
    	this.rank.put(user_id, point);
    }
    public double get_rank(){
    	double sum=0;
    	 Iterator it = rank.keySet().iterator();
         while (it.hasNext()) {
        	 Object o = it.next();
        	 sum+=rank.get(o);
         }
    	return sum/rank.size();
    }
    
    public void set_url(String url){this.url=url;}
    public String get_url(){return this.url;}
    
    public void set_title(String title){this.title=title;}
    public String get_title(){return this.title;}
    
    public void set_caption(String caption){this.caption=caption;}
    public String get_caption(){return this.caption;}
    
    public void set_user(User user){this.user=user;}
    public User get_user(){return this.user;}
    
    public void set_date(Date date){this.date=date;}
    public Date get_date(){return this.date;}
    
    public void addComment(Comment comment)	{
    	this.comments.add(comment);
    }
    
    public List<Comment> getComment() {
    	return this.comments;
    }
    
    public List<Tag> getTags() {
    	List<Tag> tags = new ArrayList<Tag>();
    	
    	List<TagPhotoRelation> tagrel = TagPhotoRelation.find("photoId = ?", this.id).fetch();
    	if (tagrel != null) {
    		for (int i = 0; i < tagrel.size(); i++) {
    			tags.add((Tag) Tag.find("id = ?", tagrel.get(i).get_tagId()).first());
    		}
    		return tags;
    	} else return tags; 
    }
    
    public Photo(String url ,String title ,String caption ,User user){
    	this.url=url;
    	this.title=title;
    	this.caption=caption;
    	this.user=user;
    	this.date=new Date();
    	this.rank=new HashMap<Long,Integer>();
    	this.comments = new ArrayList<Comment>();
    }
}
