package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rank")
public class RankDto {
    @Id
    @GeneratedValue
    private Integer id;
    private  int rankPos;
    private int price;
    private int rsEventId;
}
