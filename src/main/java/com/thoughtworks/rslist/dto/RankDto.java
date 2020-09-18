package com.thoughtworks.rslist.dto;

import lombok.*;

import javax.persistence.*;

@Entity
//@Data
@Getter
@Setter
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
