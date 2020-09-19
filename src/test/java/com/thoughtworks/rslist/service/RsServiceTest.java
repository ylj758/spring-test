package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.*;
import com.thoughtworks.rslist.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.MockitoAnnotations.initMocks;

class RsServiceTest {
  RsService rsService;

  @Mock RsEventRepository rsEventRepository;
  @Mock UserRepository userRepository;
  @Mock VoteRepository voteRepository;
  @Mock
  RankRepository rankRepository;
  @Mock RankRecordRepository rankRecordRepository;
  LocalDateTime localDateTime;
  Vote vote;

  @BeforeEach
  void setUp() {
    initMocks(this);
    rsService = new RsService(rsEventRepository, userRepository, voteRepository, rankRepository, rankRecordRepository);
    localDateTime = LocalDateTime.now();
    vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
  }

  @Test
  void should_buy_rank_when_rank_is_not_bought() throws Exception {
    when(rankRepository.findRankDtoByRankPos(anyInt())).thenReturn(Optional.empty());
    Trade trade = new Trade(5, 1);
    int buyRsEventId = 1;
    rsService.buy(trade, buyRsEventId);
//    RankDto rankDto = RankDto.builder()
//            .rsEventId(buyRsEventId)
//            .price(trade.getAmount())
//            .rankPos(trade.getRank())
//            .build();
//    RankRecordDto rankRecordDto = RankRecordDto.builder()
//            .rsEventId(buyRsEventId)
//            .price(trade.getAmount())
//            .rankPos(trade.getRank())
//            .build();
    verify(rankRepository, times(1)).save(any());
    verify(rankRecordRepository, times(1)).save(any());
  }

  @Test
  void should_buy_rank_when_rank_is_bought_and_amount_is_enough() throws Exception {
    Trade trade = new Trade(10, 1);
    int buyRsEventId = 2;
    RankDto rankDto = RankDto.builder()
            .rankPos(1)
            .price(5)
            .rsEventId(1)
            .build();
    when(rankRepository.findRankDtoByRankPos(anyInt())).thenReturn(Optional.of(rankDto));
    rsService.buy(trade, buyRsEventId);
    RankRecordDto rankRecordDto = RankRecordDto.builder().price(rankDto.getPrice()).rankPos(rankDto.getRankPos())
            .rsEventId(rankDto.getRsEventId()).build();
    verify(rankRepository, times(1)).save(rankDto);
    verify(rankRecordRepository, times(1)).save(rankRecordDto);
  }

  @Test
  void should_throw_exception_when_amount_is_not_enough() {
    Trade trade = new Trade(2, 1);
    int buyRsEventId = 2;
    RankDto rankDto = RankDto.builder()
            .rankPos(1)
            .price(5)
            .rsEventId(1)
            .build();
    when(rankRepository.findRankDtoByRankPos(anyInt())).thenReturn(Optional.of(rankDto));
    assertThrows(Exception.class, () -> rsService.buy(trade, buyRsEventId));
  }
  @Test
  void shouldVoteSuccess() {
    // given

    UserDto userDto =
        UserDto.builder()
            .voteNum(5)
            .phone("18888888888")
            .gender("female")
            .email("a@b.com")
            .age(19)
            .userName("xiaoli")
            .id(2)
            .build();
    RsEventDto rsEventDto =
        RsEventDto.builder()
            .eventName("event name")
            .id(1)
            .keyword("keyword")
            .voteNum(2)
            .user(userDto)
            .build();

    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    // when
    rsService.vote(vote, 1);
    // then
    verify(voteRepository)
        .save(
            VoteDto.builder()
                .num(2)
                .localDateTime(localDateTime)
                .user(userDto)
                .rsEvent(rsEventDto)
                .build());
    verify(userRepository).save(userDto);
    verify(rsEventRepository).save(rsEventDto);
  }

  @Test
  void shouldThrowExceptionWhenUserNotExist() {
    // given
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
    when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
    //when&then
    assertThrows(
        RuntimeException.class,
        () -> {
          rsService.vote(vote, 1);
        });
  }
}
