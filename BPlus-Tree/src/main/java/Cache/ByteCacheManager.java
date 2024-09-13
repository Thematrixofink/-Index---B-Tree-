package Cache;

import ExcelUtils.model.Index;
import cn.hutool.cache.file.LFUFileCache;
import com.alibaba.excel.EasyExcel;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * 字节缓冲区
 */
public class ByteCacheManager {

    //参数1：容量，能容纳的byte数
    //参数2：文件最大大小，byte数，决定能缓存多大的文件，大于这个值不被缓存直接读取
    //参数3：超时时间，0表示无超时时间。毫秒
    private static LFUFileCache cache = new LFUFileCache(64 * 1024, 16 * 1024, 0);


    /**
     * 从内存中获取一页Index内容
     * @param indexNum
     * @return
     */
    public static List<Index> getIndexPage(String indexNum){
        String filePath = "index/"+ indexNum + ".xlsx";
        byte[] index = cache.getFileBytes(filePath);
            List<Index> indexList = EasyExcel.read(new ByteArrayInputStream(index)).sheet().doReadSync();
            return indexList;
    }


}
