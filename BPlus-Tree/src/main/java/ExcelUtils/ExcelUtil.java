package ExcelUtils;

import Cache.EntryCacheManager;
import ExcelUtils.model.Entry;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class ExcelUtil {

    /**
     * 主键的值
     */
    private static int currentPrimaryKey = 0;


    /**
     * 向Excel写数据
     */
    public void genRandomEntry() {
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        EasyExcel.write("entry/0"+(currentPrimaryKey+1)+".xlsx", Entry.class)
                .sheet("1")
                .doWrite(() -> {
                    // 分页查询数据
                    return getExcelData();
                });
    }


    /**
     * 从内存中读取一个数据页
     * @param fileName
     * @return 返回读取到的数据
     */
    public List<Entry> readEntry(String fileName) {
        // 读取256条数据
        List<Entry> list = EasyExcel.read(fileName, Entry.class, new PageReadListener<Entry>(dataList -> {
        }, 256)).sheet().doReadSync();
        return list;
    }

    /**
     * 模拟生成表格数据
     * @return
     */
    private List<Entry> getExcelData() {
        List<Entry> list = ListUtils.newArrayList();
        //一条数据64B，大小为16KB，则一个数据页可以有256条记录
        for (int i = 0; i < 256; i++) {
            Entry data = new Entry();
            data.setRecord(generateRecord());
            data.setPrimaryKey(generatePrimaryKey());
            list.add(data);
        }
        return list;
    }

    /**
     * 递增生成主键
     * @return
     */
    private static Integer generatePrimaryKey() {
        currentPrimaryKey++;
        return currentPrimaryKey;
    }

    /**
     * 生成记录
     * @return
     */
    private static String generateRecord() {
        String key = Integer.toString(currentPrimaryKey);
        byte[] bytes = key.getBytes();
        int length = bytes.length;
        int stringLength = 64 - length;
        return RandomUtil.randomString(stringLength);
    }




    private static void genRandomData(){
        EntryCacheManager.getFromCache("1", Character.class);
    }



}




