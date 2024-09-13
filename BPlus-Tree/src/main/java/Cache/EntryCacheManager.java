package Cache;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;

import java.util.List;

public class EntryCacheManager {

    //缓存 ,最多可以容纳 4 * 16KB
    public  static Cache<String, List> cache = CacheUtil.newLRUCache(4);

    /**
     * 从Cache中获取数据，如果不存在的话，那么就从磁盘读取
     * @param key 文件的数字编号
     * @param clazz 文件要映射的类
     * @return
     */
    public static List getFromCache(String key,Class clazz){
        String indexFilePath = System.getProperty("user.dir")+"/index/"+key+".xlsx";
        String entryFilePath = System.getProperty("user.dir")+"/entry/"+key+".xlsx";
        if(cache.containsKey(key)){
            return cache.get(key);
        }else{
            if(FileUtil.exist(indexFilePath)){
                List objects = EasyExcel.read(indexFilePath, clazz, new PageReadListener<>(a -> {
                })).sheet().doReadSync();
                cache.put(key,objects);
            }else if(FileUtil.exist(entryFilePath)){
                List objects = EasyExcel.read(entryFilePath, clazz, new PageReadListener<>(a -> {
                })).sheet().doReadSync();
                cache.put(key,objects);
            }else{
                return null;
            }
        }
        return cache.get(key);
    }

    public static void clearCache(){
        cache.clear();
    }




}

