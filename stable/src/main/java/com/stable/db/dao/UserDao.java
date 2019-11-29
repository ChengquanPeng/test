/*package com.stable.db.dao;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stable.vo.UserVo;

//@Repository
public class UserDao {
	@Autowired
    private JdbcTemplate jdbcTemplate;
    
	public UserVo getUserById(Integer id) {
        List<UserVo> list = jdbcTemplate.query("select * from tb_user where id = ?", new Object[]{id}, new BeanPropertyRowMapper<UserVo>(UserVo.class));
        if(list!=null && list.size()>0){
            return list.get(0);
        }else{
            return null;
        }
    }
 
    public List<UserVo> getUserList() {
        List<UserVo> list = jdbcTemplate.query("select * from tb_user", new Object[]{}, new BeanPropertyRowMapper<UserVo>(UserVo.class));
        if(list!=null && list.size()>0){
            return list;
        }else{
            return null;
        }
    }
 
    public int add(UserVo UserVo) {
        return jdbcTemplate.update("insert into tb_user(username, age, ctm) values(?, ?, ?)",
                UserVo.getUsername(),UserVo.getAge(), new Date());
    }
 
    public int update(Integer id, UserVo UserVo) {
        return jdbcTemplate.update("UPDATE tb_user SET username = ? , age = ? WHERE id=?",
                UserVo.getUsername(),UserVo.getAge(), id);
    }
 
    public int delete(Integer id) {
        return jdbcTemplate.update("DELETE from tb_user where id = ? ",id);
    }
}
*/