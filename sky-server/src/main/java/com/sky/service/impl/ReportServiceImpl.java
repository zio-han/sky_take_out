package com.sky.service.impl;


import com.alibaba.fastjson.JSONArray;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end里的所有日期
        List<LocalDate> dateList = new ArrayList<>();

        //将begin到end之间的所有日期放入dateList集合中
        dateList.add(begin);
        while(begin.isBefore(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        //存放每天对应的营业额数据
        List<Double> turnOverList = new ArrayList<>();

        for(LocalDate date : dateList){
            //查询date日期对应的营业额数据，营业额是指：状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //此处省略了查询数据库的操作
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnOver = orderMapper.sumByMap(map);

            turnOver = turnOver == null ? 0.0 : turnOver;
            //将每天的营业额数据放入turnOverList集合中
            turnOverList.add(turnOver);
        }



        //封装返回结果
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnOverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUsersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        //存放从begin到end对应的日期
        dateList.add(begin);
        while(begin.isBefore(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天对应的用户总量 select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();
        //存放每天增加的用户数量 select count(id) from user where create_time > ? and create_time < ?
        List<Integer> newUserList = new ArrayList<>();

        for(LocalDate date : dateList){

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);
            //总用户数量
            Integer totalUser = userMapper.countByMap(map);

            totalUser = totalUser == null ? 0:totalUser;
            totalUserList.add(totalUser);
            map.put("begin", beginTime);

            Integer newUser = userMapper.countByMap(map);
            newUser = newUser == null ? 0:newUser;
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 统计指定时间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(begin.isBefore(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的订单总数
        List <Integer> orderCountList = new ArrayList();

        //存放每天的有效订单总数
        List <Integer> validOrderCountList = new ArrayList();

        //遍历dateList查询每日订单数和有效订单数
        for(LocalDate date : dateList){
           LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
           LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

           Integer orderCount = getOrderCount(beginTime, endTime, null);

           Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

           orderCountList.add(orderCount);
           validOrderCountList.add(validOrderCount);
        }
        //计算时间区间内的订单总数量
        Integer totalOrderSum = orderCountList.stream()
                .reduce(Integer::sum)
                .orElse(0);

        //计算时间区间内的有效订单总数量

        Integer validOrderSum = validOrderCountList.stream()
                .reduce(Integer::sum)
                .orElse(0);

        //订单总有效率
        Double orderCompletionRate = validOrderSum * 1.0 / totalOrderSum;

        orderCompletionRate = totalOrderSum == 0 ? 0.0 : orderCompletionRate;
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderSum)
                .validOrderCount(validOrderSum)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }

    /**
     * 查询top10的菜品
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
       LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
       LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);
       List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime,endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());

        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        Integer count = orderMapper.countByMap(map);
        return count;
    }

}
