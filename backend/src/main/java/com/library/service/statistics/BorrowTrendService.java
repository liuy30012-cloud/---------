package com.library.service.statistics;

import com.library.dto.BorrowTrendDTO;
import com.library.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 借阅趋势分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowTrendService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final Clock clock;

    /**
     * 获取借阅趋势（最近N天）
     * @param days 天数
     * @return 借阅趋势列表
     */
    public List<BorrowTrendDTO> getBorrowTrends(int days) {
        LocalDateTime startDate = LocalDateTime.now(clock).minusDays(days);

        // 使用数据库聚合查询，避免加载所有记录到内存
        List<Object[]> borrowByDateList = borrowRecordRepository.countBorrowsByDate(startDate);
        List<Object[]> returnByDateList = borrowRecordRepository.countReturnsByDate(startDate);

        // 转换为Map（安全类型转换，兼容不同JPA Provider的返回类型）
        Map<LocalDate, Long> borrowByDate = new HashMap<>();
        for (Object[] arr : borrowByDateList) {
            borrowByDate.put(DateConversionUtil.convertToLocalDate(arr[0]), (Long) arr[1]);
        }

        Map<LocalDate, Long> returnByDate = new HashMap<>();
        for (Object[] arr : returnByDateList) {
            returnByDate.put(DateConversionUtil.convertToLocalDate(arr[0]), (Long) arr[1]);
        }

        // 生成趋势数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<BorrowTrendDTO> trends = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now(clock).minusDays(i);
            String dateStr = date.format(formatter);
            Long borrowCount = borrowByDate.getOrDefault(date, 0L);
            Long returnCount = returnByDate.getOrDefault(date, 0L);
            trends.add(new BorrowTrendDTO(dateStr, borrowCount, returnCount));
        }

        return trends;
    }
}
