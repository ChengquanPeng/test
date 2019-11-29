/*package com.stable.db.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stable.vo.bus.StockBaseInfo;

//@Repository
public class DbStockBaseInfoDao {
	@Autowired
    private JdbcTemplate jdbcTemplate;
    
	public StockBaseInfo getById(String code) {
        List<StockBaseInfo> list = jdbcTemplate.query("select * from stock_base_info where code = ?", new Object[]{code}, new BeanPropertyRowMapper<StockBaseInfo>(StockBaseInfo.class));
        if(list!=null && list.size()>0){
            return list.get(0);
        }else{
            return null;
        }
    }
 
    public List<StockBaseInfo> getList() {
        List<StockBaseInfo> list = jdbcTemplate.query("select * from stock_base_info", new Object[]{}, new BeanPropertyRowMapper<StockBaseInfo>(StockBaseInfo.class));
        if(list!=null && list.size()>0){
            return list;
        }else{
            return null;
        }
    }
    
    public List<StockBaseInfo> getListWithOnStauts() {
        List<StockBaseInfo> list = jdbcTemplate.query("select * from stock_base_info where list_status='L' ", new Object[]{}, new BeanPropertyRowMapper<StockBaseInfo>(StockBaseInfo.class));
        if(list!=null && list.size()>0){
            return list;
        }else{
            return null;
        }
    }
 
    public int insert(StockBaseInfo b) {
        return jdbcTemplate.update("INSERT INTO stock_base_info (code, ts_code, name, area, industry, fullname, enname, market, exchange, curr_type, list_status, list_date, delist_date, is_hs,udp_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,NOW())",
                b.getCode(),b.getTs_code(),b.getName(),b.getArea(),b.getIndustry(),b.getFullname(),b.getEnname(),b.getMarket(),b.getExchange(),b.getCurr_type(),b.getList_status(),b.getList_date(),b.getDelist_date(),b.getIs_hs());
    }
 
    public int update(StockBaseInfo b) {
        return jdbcTemplate.update("UPDATE stock_base_info SET name=?, area=?, industry=?, fullname=?, enname=?, market=?, exchange=?, curr_type=?, list_status=?, list_date=?, delist_date=?, is_hs=?,udp_date=NOW() WHERE code=?",
        		b.getName(),b.getArea(),b.getIndustry(),b.getFullname(),b.getEnname(),b.getMarket(),b.getExchange(),b.getCurr_type(),b.getList_status(),b.getList_date(),b.getDelist_date(),b.getIs_hs(),b.getCode());
    }
 
    private boolean exitis(String code) {
    	return (jdbcTemplate.queryForObject("select count(code) from stock_base_info where code = ?",new Object[]{code}, Integer.class)>0);
    }
    
    public int saveOrUpdate(StockBaseInfo b) {
        if (exitis(b.getCode())) {
			return this.update(b);
		}else {
			return this.insert(b);
		}
    }
}
*/