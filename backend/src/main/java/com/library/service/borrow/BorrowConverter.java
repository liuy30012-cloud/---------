package com.library.service.borrow;

import com.library.dto.BorrowResponse;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BorrowConverter {

    private final BookService bookService;

    public BorrowResponse toResponse(BorrowRecord record) {
        Book book = bookService.getBookById(record.getBookId());

        BorrowResponse response = new BorrowResponse();
        response.setId(record.getId());
        response.setBookId(record.getBookId());
        response.setBookTitle(record.getBookTitle());
        response.setBookIsbn(record.getBookIsbn());
        response.setBorrowDate(record.getBorrowDate());
        response.setDueDate(record.getDueDate());
        response.setReturnDate(record.getReturnDate());
        response.setStatus(record.getStatus().name());
        response.setRenewed(record.getRenewed());
        response.setRenewCount(record.getRenewCount());
        response.setOverdueDays(record.getOverdueDays());
        response.setFineAmount(record.getFineAmount());
        response.setFinePaid(record.getFinePaid());
        response.setNotes(record.getNotes());
        response.setApprovedAt(record.getApprovedAt());
        response.setRejectReason(record.getRejectReason());
        response.setPickupDeadline(record.getPickupDeadline());
        response.setCreatedAt(record.getCreatedAt());

        if (book != null) {
            response.setCoverUrl(book.getCoverUrl());
            response.setLocation(book.getLocation());
            response.setCirculationPolicy(book.getCirculationPolicy().name());
        }

        response.setNextAction(resolveNextAction(record));
        response.setStatusHint(resolveStatusHint(record));
        return response;
    }

    private String resolveNextAction(BorrowRecord record) {
        return switch (record.getStatus()) {
            case PENDING -> "WAIT_APPROVAL";
            case APPROVED -> "PICKUP";
            case BORROWED -> "RETURN_OR_RENEW";
            case OVERDUE -> "RETURN_NOW";
            case REJECTED -> "CONTACT_LIBRARY";
            case RETURNED -> "BROWSE_MORE";
        };
    }

    private String resolveStatusHint(BorrowRecord record) {
        return switch (record.getStatus()) {
            case PENDING -> "等待管理员审核。";
            case APPROVED -> "审核已通过，请及时取书。";
            case BORROWED -> "当前借阅进行中。";
            case OVERDUE -> "当前借阅已逾期。";
            case REJECTED -> record.getRejectReason() == null
                ? "借阅申请未通过。"
                : "借阅申请未通过：" + record.getRejectReason();
            case RETURNED -> "图书已归还。";
        };
    }
}
