package com.dot.dto;

import com.dot.entity.KptItem;
import com.dot.entity.Retrospect;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class RetrospectDto {

    @Getter
    @Setter
    public static class ScoreRequest {
        @NotNull
        @Min(1) @Max(5)
        private Integer score;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private String content;

        public static ItemResponse from(KptItem item) {
            return ItemResponse.builder().id(item.getId()).content(item.getContent()).build();
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private LocalDate date;
        private Integer score;
        private String colorTheme;
        private Boolean isGithubSynced;
        private List<ItemResponse> keep;
        private List<ItemResponse> problem;
        private List<ItemResponse> tryItems;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Retrospect r) {
            Comparator<KptItem> byOrder = Comparator.comparing(KptItem::getOrderIndex);
            return Response.builder()
                    .id(r.getId())
                    .date(r.getDate())
                    .score(r.getScore())
                    .colorTheme(r.getColorTheme())
                    .isGithubSynced(r.getIsGithubSynced())
                    .keep(r.getItems().stream()
                            .filter(i -> i.getType() == KptItem.Type.KEEP)
                            .sorted(byOrder).map(ItemResponse::from).toList())
                    .problem(r.getItems().stream()
                            .filter(i -> i.getType() == KptItem.Type.PROBLEM)
                            .sorted(byOrder).map(ItemResponse::from).toList())
                    .tryItems(r.getItems().stream()
                            .filter(i -> i.getType() == KptItem.Type.TRY)
                            .sorted(byOrder).map(ItemResponse::from).toList())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CalendarItem {
        private Long id;
        private LocalDate date;
        private Integer score;
        private String colorTheme;
    }

    @Getter
    @Setter
    public static class ItemCreateRequest {
        @NotNull
        private KptItem.Type type;
        @NotNull
        private String content;
    }

    @Getter
    @Setter
    public static class ItemUpdateRequest {
        @NotNull
        private String content;
    }

    @Getter
    @Setter
    public static class ItemOrderRequest {
        @NotNull
        private KptItem.Type type;
        @NotNull
        private List<Long> orderedIds;
    }
}
