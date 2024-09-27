package com.example.coconote.api.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasBlockDocument {
    @Id
    private String id;  // OpenSearch 문서 ID
    private String type;  // "canvas" 또는 "block"

    // Canvas 관련 필드
    private String canvasTitle;
    private String createMemberName;
    private Long channelId;
    private String  canvasCreatedTime;

    // Block 관련 필드
    private String blockContents;
    private String blockMember;
    private String canvasId;  // Block이 속한 Canvas ID
    private String prevBlockId;
    private String parentBlockId;
    private String feId;
    private String blockType;
}
