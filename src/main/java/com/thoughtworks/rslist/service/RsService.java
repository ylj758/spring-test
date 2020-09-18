package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.*;
import com.thoughtworks.rslist.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RsService {
    final RsEventRepository rsEventRepository;
    final UserRepository userRepository;
    final VoteRepository voteRepository;
    final RankDtoRepository rankDtoRepository;
    final RankRecordRepository rankRecordRepository;

    public RsService(RsEventRepository rsEventRepository, UserRepository userRepository,
                     VoteRepository voteRepository, RankDtoRepository rankDtoRepository,
                     RankRecordRepository rankRecordRepository) {
        this.rsEventRepository = rsEventRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.rankDtoRepository = rankDtoRepository;
        this.rankRecordRepository = rankRecordRepository;
    }

    public void vote(Vote vote, int rsEventId) {
        Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
        Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
        if (!rsEventDto.isPresent()
                || !userDto.isPresent()
                || vote.getVoteNum() > userDto.get().getVoteNum()) {
            throw new RuntimeException();
        }
        VoteDto voteDto =
                VoteDto.builder()
                        .localDateTime(vote.getTime())
                        .num(vote.getVoteNum())
                        .rsEvent(rsEventDto.get())
                        .user(userDto.get())
                        .build();
        voteRepository.save(voteDto);
        UserDto user = userDto.get();
        user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
        userRepository.save(user);
        RsEventDto rsEvent = rsEventDto.get();
        rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
        rsEventRepository.save(rsEvent);
    }


    public void buy(Trade trade, int eventId) throws Exception {
        int rank = trade.getRank();
        Optional<RankDto> optionalRankDto = rankDtoRepository.findRankDtoByRankPos(rank);
        if (!optionalRankDto.isPresent()) {
            RankDto rankDto = RankDto.builder().rankPos(rank).price(trade.getAmount()).rsEventId(eventId).build();
            rankDtoRepository.save(rankDto);
            rankRecordRepository.save(rankDtoConvertRankRecordDto(rankDto));
        } else {
            RankDto rankDto = optionalRankDto.get();
            if (trade.getAmount() > rankDto.getPrice()) {
                rsEventRepository.deleteById(rankDto.getRsEventId());
                rankDto.setRsEventId(eventId);
                rankDto.setPrice(trade.getAmount());
                rankDtoRepository.save(rankDto);
                rankRecordRepository.save(rankDtoConvertRankRecordDto(rankDto));
            } else {
                throw new Exception("buy rank failed");
            }
        }
    }

    public RankRecordDto rankDtoConvertRankRecordDto(RankDto rankDto){
       return RankRecordDto.builder().price(rankDto.getPrice()).rankPos(rankDto.getRankPos())
                .rsEventId(rankDto.getRsEventId()).build();
    }
}
