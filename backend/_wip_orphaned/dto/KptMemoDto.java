package com.dot.dto;

import com.dot.entity.KptMemo;
import com.dot.entity.KptType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class KptMemoDto {

    @Getter
    @Setter
    public static class CreateRequest {
        @NotNull
        private KptType type;
        @NotBlank
        private String content;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String content;
    }

    @Getter
    @Setter
    public static class OrderRequest {
        @NotNull
        private KptType type;
        @NotNull
        private List<Long> orderedIds;
    }

    @Getter
    @Builder
    public static class Item {
        private Long id;
        private String content;
        @JsonProperty("order")
        private Integer orderIndex;

        public static Item from(KptMemo m) {
            return Item.builder()
                    .id(m.getId())
                    .content(m.getContent())
                    .orderIndex(m.getOrderIndex())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ListResponse {
        private List<Item> keep;
        private List<Item> problem;
        @JsonProperty("try")
        private List<Item> tryItems;
    }
}
