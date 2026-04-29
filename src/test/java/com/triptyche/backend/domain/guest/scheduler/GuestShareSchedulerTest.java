package com.triptyche.backend.domain.guest.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.triptyche.backend.domain.guest.repository.GuestShareQueueRepository;
import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.config.GuestProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestShareSchedulerTest {

    @Mock private GuestShareQueueRepository guestShareQueueRepository;
    @Mock private UserRepository userRepository;
    @Mock private TripRepository tripRepository;
    @Mock private ShareService shareService;
    @Mock private GuestProperties guestProperties;

    @InjectMocks
    private GuestShareScheduler guestShareScheduler;

    @Nested
    @DisplayName("sendShareToReadyGuests()")
    class SendShareToReadyGuests {

        @Test
        @DisplayName("대기 중인 게스트가 없으면 아무것도 하지 않는다")
        void givenEmptyQueue_doesNothing() {
            given(guestShareQueueRepository.pollDueIds()).willReturn(List.of());

            guestShareScheduler.sendShareToReadyGuests();

            then(userRepository).shouldHaveNoInteractions();
            then(shareService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("템플릿 계정이 없으면 공유 신청을 하지 않는다")
        void givenNoTemplateUser_doesNotCreateShare() {
            given(guestShareQueueRepository.pollDueIds()).willReturn(List.of(1L));
            given(guestProperties.templateEmail()).willReturn("template@triptyche.com");
            given(userRepository.findByUserEmail("template@triptyche.com")).willReturn(Optional.empty());

            guestShareScheduler.sendShareToReadyGuests();

            then(shareService).should(never()).createShare(any(), any());
        }

        @Test
        @DisplayName("공유 대상 트립이 템플릿 트립 목록에 없으면 공유 신청을 하지 않는다")
        void givenTargetTripNotFound_doesNotCreateShare() {
            given(guestShareQueueRepository.pollDueIds()).willReturn(List.of(1L));

            User templateUser = User.builder().userId(99L).userEmail("template@triptyche.com").build();
            given(guestProperties.templateEmail()).willReturn("template@triptyche.com");
            given(userRepository.findByUserEmail("template@triptyche.com")).willReturn(Optional.of(templateUser));

            Trip wrongTrip = mock(Trip.class);
            given(wrongTrip.getTripTitle()).willReturn("도쿄 봄 여행");
            given(tripRepository.findAllByUser(templateUser)).willReturn(List.of(wrongTrip));
            given(guestProperties.shareTargetTripTitle()).willReturn("뉴욕 가을 여행");

            guestShareScheduler.sendShareToReadyGuests();

            then(shareService).should(never()).createShare(any(), any());
        }

        @Test
        @DisplayName("공유 대상 트립이 존재하면 대기 중인 각 게스트에게 공유 신청한다")
        void givenTargetTripFound_createsShareForEachGuest() {
            given(guestShareQueueRepository.pollDueIds()).willReturn(List.of(1L, 2L));

            User templateUser = User.builder().userId(99L).userEmail("template@triptyche.com").build();
            given(guestProperties.templateEmail()).willReturn("template@triptyche.com");
            given(userRepository.findByUserEmail("template@triptyche.com")).willReturn(Optional.of(templateUser));

            Trip targetTrip = mock(Trip.class);
            given(targetTrip.getTripTitle()).willReturn("뉴욕 가을 여행");
            given(targetTrip.getTripKey()).willReturn("nyc-key");
            given(tripRepository.findAllByUser(templateUser)).willReturn(List.of(targetTrip));
            given(guestProperties.shareTargetTripTitle()).willReturn("뉴욕 가을 여행");

            guestShareScheduler.sendShareToReadyGuests();

            then(shareService).should(org.mockito.Mockito.times(2)).createShare(any(), any());
        }
    }
}
