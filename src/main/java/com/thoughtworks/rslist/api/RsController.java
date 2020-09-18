package com.thoughtworks.rslist.api;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RankDto;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.exception.Error;
import com.thoughtworks.rslist.exception.RequestNotValidException;
import com.thoughtworks.rslist.repository.RankRecordRepository;
import com.thoughtworks.rslist.repository.RankRepository;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.service.RsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Validated
public class RsController {
    @Autowired
    RsEventRepository rsEventRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RsService rsService;
    @Autowired
    RankRepository rankRepository;
    @Autowired
    RankRecordRepository rankRecordRepository;

    @GetMapping("/rs/list")
    public ResponseEntity<List<RsEvent>> getRsEventListBetween(
            @RequestParam(required = false) Integer start, @RequestParam(required = false) Integer end) {
        List<RsEvent> rsEventDtoList =
                rsEventRepository.findAll().stream()
                        .map(
                                item ->
                                        RsEvent.builder()
                                                .eventName(item.getEventName())
                                                .keyword(item.getKeyword())
                                                .userId(item.getId())
                                                .voteNum(item.getVoteNum())
                                                .build()).collect(Collectors.toList());
//        List<RsEventDto> rsEventDtoList = rsEventRepository.findAll();

        List<RankDto> rankDtoList = rankRepository.findAll();
        List<RsEvent> sortRsEventDtoList = new ArrayList<>(rsEventDtoList.size());
        for (int i = 0; i < rsEventDtoList.size(); i++) {
            sortRsEventDtoList.add(null);
        }
        for(int i=0; i < rankDtoList.size(); i++){
            RankDto rankDto = rankDtoList.get(i);
            Optional<RsEventDto> rsEventOptional = rsEventRepository.findById(rankDto.getRsEventId());
            RsEventDto rsEventDto = rsEventOptional.get();
            sortRsEventDtoList.set(rankDto.getRankPos() - 1, RsEventDtoConvertRsEvent(rsEventDto));
            rsEventDtoList.remove(rsEventDto);
        }
        List<RsEvent> leftRsEventDtoSortList = rsEventDtoList.stream().sorted(Comparator.comparing(RsEvent::getVoteNum).reversed()).collect(Collectors.toList());
        for (int i = 0; i < sortRsEventDtoList.size(); i++) {
            if (sortRsEventDtoList.get(i) == null) {
                sortRsEventDtoList.set(i, leftRsEventDtoSortList.remove(0));
            }
        }
        if (start == null || end == null) {
            return ResponseEntity.ok(sortRsEventDtoList);
        }
        return ResponseEntity.ok(sortRsEventDtoList.subList(start - 1, end));
}

    @GetMapping("/rs/{index}")
    public ResponseEntity<RsEvent> getRsEvent(@PathVariable int index) {
        List<RsEvent> rsEvents =
                rsEventRepository.findAll().stream()
                        .map(
                                item ->
                                        RsEvent.builder()
                                                .eventName(item.getEventName())
                                                .keyword(item.getKeyword())
                                                .userId(item.getId())
                                                .voteNum(item.getVoteNum())
                                                .build())
                        .collect(Collectors.toList());
        if (index < 1 || index > rsEvents.size()) {
            throw new RequestNotValidException("invalid index");
        }
        return ResponseEntity.ok(rsEvents.get(index - 1));
    }

    @PostMapping("/rs/event")
    public ResponseEntity addRsEvent(@RequestBody @Valid RsEvent rsEvent) {
        Optional<UserDto> userDto = userRepository.findById(rsEvent.getUserId());
        if (!userDto.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        RsEventDto build =
                RsEventDto.builder()
                        .keyword(rsEvent.getKeyword())
                        .eventName(rsEvent.getEventName())
                        .voteNum(0)
                        .user(userDto.get())
                        .build();
        rsEventRepository.save(build);
        return ResponseEntity.created(null).build();
    }

    @PostMapping("/rs/vote/{id}")
    public ResponseEntity vote(@PathVariable int id, @RequestBody Vote vote) {
        rsService.vote(vote, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rs/buy/{id}")
    @Transactional
    public ResponseEntity buy(@PathVariable int id, @RequestBody Trade trade) throws Exception {
        try {
            rsService.buy(trade, id);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }


    @ExceptionHandler(RequestNotValidException.class)
    public ResponseEntity<Error> handleRequestErrorHandler(RequestNotValidException e) {
        Error error = new Error();
        error.setError(e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    public RsEvent RsEventDtoConvertRsEvent(RsEventDto rsEventDto){
        return RsEvent.builder()
                .eventName(rsEventDto.getEventName())
                .keyword(rsEventDto.getKeyword())
                .voteNum(rsEventDto.getVoteNum())
                .userId(rsEventDto.getUser().getId())
                .build();
    }
}
