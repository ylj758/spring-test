package com.thoughtworks.rslist.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.dto.*;
import com.thoughtworks.rslist.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RsEventRepository rsEventRepository;
    @Autowired
    VoteRepository voteRepository;
    @Autowired
    RankRepository rankRepository;
    @Autowired
    RankRecordRepository rankRecordRepository;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto =
                UserDto.builder()
                        .voteNum(10)
                        .phone("188888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("idolice")
                        .build();
    }

    @AfterEach
    void tearDown(){
        voteRepository.deleteAll();
        rsEventRepository.deleteAll();
        userRepository.deleteAll();
        rankRecordRepository.deleteAll();
        rankRepository.deleteAll();
    }

    @Test
    void should_buy_rank_when_rank_is_not_bought() throws Exception {
        Trade trade = new Trade(5, 1);
        mockMvc.perform(post("/rs/buy/1")
                .content(new ObjectMapper().writeValueAsString(trade))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertEquals(rankRepository.findAll().size(), 1);
        assertEquals(rankRecordRepository.findAll().size(), 1);
    }

    @Test
    void should_buy_rank_when_rank_is_bought_and_amount_is_enough() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto1 =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();
        RsEventDto rsEventDto2 =
                RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto1);
        rsEventRepository.save(rsEventDto2);
        RankDto rankDto = RankDto.builder().rankPos(1).rsEventId(2).price(5).build();
        rankRepository.save(rankDto);
        RankRecordDto rankRecordDto = RankRecordDto.builder().rankPos(1).rsEventId(2).price(5).build();
        rankRecordRepository.save(rankRecordDto);

        Trade trade = new Trade(10, 1);
        mockMvc.perform(post("/rs/buy/3")
                .content(new ObjectMapper().writeValueAsString(trade))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertEquals(rankRepository.findAll().size(), 1);
        assertEquals(rankRecordRepository.findAll().size(), 2);
        assertEquals(rsEventRepository.findById(3).get().getEventName(), "第二条事件");
        assertEquals(rsEventRepository.findById(2).isPresent(), false);
        assertEquals(rankRepository.findById(4).get().getPrice(), 10);
        assertEquals(rankRepository.findById(4).get().getRsEventId(), 3);

    }

    @Test
    void should_throw_exception_when_amount_is_not_enough() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto1 =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();
        RsEventDto rsEventDto2 =
                RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto1);
        rsEventRepository.save(rsEventDto2);
        RankDto rankDto = RankDto.builder().rankPos(1).rsEventId(2).price(5).build();
        rankRepository.save(rankDto);
        RankRecordDto rankRecordDto = RankRecordDto.builder().rankPos(1).rsEventId(2).price(5).build();
        rankRecordRepository.save(rankRecordDto);

        Trade trade = new Trade(2, 1);
        mockMvc.perform(post("/rs/buy/3")
                .content(new ObjectMapper().writeValueAsString(trade))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_get_rs_event_list_sorted_by_rank_and_votNum_start_end_null() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto1 = RsEventDto.builder().keyword("无分类").eventName("第二条事件").voteNum(10).user(save).build();
        RsEventDto rsEventDto2 = RsEventDto.builder().keyword("无分类").eventName("第三条事件").voteNum(4).user(save).build();
        RsEventDto rsEventDto3 = RsEventDto.builder().keyword("无分类").eventName("第四条事件").voteNum(2).user(save).build();
        RsEventDto rsEventDto4 = RsEventDto.builder().keyword("无分类").eventName("第五条事件").voteNum(0).user(save).build();
        rsEventRepository.save(rsEventDto1);
        rsEventRepository.save(rsEventDto2);
        rsEventRepository.save(rsEventDto3);
        rsEventRepository.save(rsEventDto4);
        RankDto rankDto1 = RankDto.builder().rankPos(1).rsEventId(4).price(5).build();
        RankDto rankDto2 = RankDto.builder().rankPos(3).rsEventId(5).price(5).build();
        rankRepository.save(rankDto1);
        rankRepository.save(rankDto2);

        mockMvc.perform(get("/rs/list"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].eventName", is("第四条事件")))
                .andExpect(jsonPath("$[0].voteNum", is(2)))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].voteNum", is(10)))
                .andExpect(jsonPath("$[2].eventName", is("第五条事件")))
                .andExpect(jsonPath("$[2].voteNum", is(0)))
                .andExpect(jsonPath("$[3].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[3].voteNum", is(4)))
                .andExpect(status().isOk());
    }

    @Test
    public void should_get_rs_event_list_sorted_by_rank_and_votNum_start_end_not_null() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto1 = RsEventDto.builder().keyword("无分类").eventName("第二条事件").voteNum(10).user(save).build();
        RsEventDto rsEventDto2 = RsEventDto.builder().keyword("无分类").eventName("第三条事件").voteNum(4).user(save).build();
        RsEventDto rsEventDto3 = RsEventDto.builder().keyword("无分类").eventName("第四条事件").voteNum(2).user(save).build();
        RsEventDto rsEventDto4 = RsEventDto.builder().keyword("无分类").eventName("第五条事件").voteNum(0).user(save).build();
        rsEventRepository.save(rsEventDto1);
        rsEventRepository.save(rsEventDto2);
        rsEventRepository.save(rsEventDto3);
        rsEventRepository.save(rsEventDto4);
        RankDto rankDto1 = RankDto.builder().rankPos(1).rsEventId(4).price(5).build();
        RankDto rankDto2 = RankDto.builder().rankPos(3).rsEventId(5).price(5).build();
        rankRepository.save(rankDto1);
        rankRepository.save(rankDto2);

        mockMvc.perform(get("/rs/list?start=1&end=3"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].eventName", is("第四条事件")))
                .andExpect(jsonPath("$[0].voteNum", is(2)))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].voteNum", is(10)))
                .andExpect(jsonPath("$[2].eventName", is("第五条事件")))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetOneEvent() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.eventName", is("第一条事件")));
        mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.keyword", is("无分类")));
        mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.eventName", is("第二条事件")));
        mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.keyword", is("无分类")));
    }

    @Test
    public void shouldGetErrorWhenIndexInvalid() throws Exception {
        mockMvc
                .perform(get("/rs/4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid index")));
    }

    @Test
    public void shouldGetRsListBetween() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第三条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        mockMvc
                .perform(get("/rs/list?start=1&end=2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventName", is("第一条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")));
        mockMvc
                .perform(get("/rs/list?start=2&end=3"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")));
        mockMvc
                .perform(get("/rs/list?start=1&end=3"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")))
                .andExpect(jsonPath("$[2].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[2].keyword", is("无分类")));
    }

    @Test
    public void shouldAddRsEventWhenUserExist() throws Exception {

        UserDto save = userRepository.save(userDto);

        String jsonValue =
                "{\"eventName\":\"猪肉涨价了\",\"keyword\":\"经济\",\"userId\": " + save.getId() + "}";

        mockMvc
                .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
        List<RsEventDto> all = rsEventRepository.findAll();
        assertNotNull(all);
        assertEquals(all.size(), 1);
        assertEquals(all.get(0).getEventName(), "猪肉涨价了");
        assertEquals(all.get(0).getKeyword(), "经济");
        assertEquals(all.get(0).getUser().getUserName(), save.getUserName());
        assertEquals(all.get(0).getUser().getAge(), save.getAge());
    }

    @Test
    public void shouldAddRsEventWhenUserNotExist() throws Exception {
        String jsonValue = "{\"eventName\":\"猪肉涨价了\",\"keyword\":\"经济\",\"userId\": 100}";
        mockMvc
                .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVoteSuccess() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();
        rsEventDto = rsEventRepository.save(rsEventDto);

        String jsonValue =
                String.format(
                        "{\"userId\":%d,\"time\":\"%s\",\"voteNum\":1}",
                        save.getId(), LocalDateTime.now().toString());
        mockMvc
                .perform(
                        post("/rs/vote/{id}", rsEventDto.getId())
                                .content(jsonValue)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        UserDto userDto = userRepository.findById(save.getId()).get();
        RsEventDto newRsEvent = rsEventRepository.findById(rsEventDto.getId()).get();
        assertEquals(userDto.getVoteNum(), 9);
        assertEquals(newRsEvent.getVoteNum(), 1);
        List<VoteDto> voteDtos = voteRepository.findAll();
        assertEquals(voteDtos.size(), 1);
        assertEquals(voteDtos.get(0).getNum(), 1);
    }
}
