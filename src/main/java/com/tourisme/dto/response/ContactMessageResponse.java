package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessageResponse {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private Boolean readByAdmin;
    private String adminReply;
    private LocalDateTime repliedAt;
    /** Non-null only on POST …/reply: whether an email was sent to the visitor. */
    private Boolean replyEmailDelivered;
    private LocalDateTime createdAt;
    /** Full thread history (old rows are backfilled on migration). */
    private List<ContactThreadMessageResponse> thread;
}
