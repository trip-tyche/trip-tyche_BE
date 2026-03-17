package com.triptyche.backend.domain.trip.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.triptyche.backend.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TripTest {

  private Trip createTrip(TripStatus status) {
    return Trip.builder()
            .tripTitle("테스트 여행")
            .country("일본")
            .hashtags("여행,테스트")
            .status(status)
            .build();
  }

  @Nested
  @DisplayName("markImagesUploaded()")
  class MarkImagesUploaded {

    @Test
    @DisplayName("DRAFT 상태에서 호출하면 IMAGES_UPLOADED로 변경된다")
    void success() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.DRAFT);

      //when
      trip.markImagesUploaded();

      //then
      assertThat(trip.getStatus()).isEqualTo(TripStatus.IMAGES_UPLOADED);
    }

    @Test
    @DisplayName("DRAFT가 아닌 상태에서 호출하면 예외 발생")
    void fail_when_not_draft() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.IMAGES_UPLOADED);

      //when & then
      assertThatThrownBy(trip::markImagesUploaded)
              .isInstanceOf(CustomException.class);
    }
  }

  @Nested
  @DisplayName("confirmTrip()")
  class ConfirmTrip {

    @Test
    @DisplayName("IMAGES_UPLOADED 상태에서 호출하면 CONFIRMED로 변경된다")
    void success() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.IMAGES_UPLOADED);

      //when
      trip.confirmTrip();

      //then
      assertThat(trip.getStatus()).isEqualTo(TripStatus.CONFIRMED);
    }

    @Test
    @DisplayName("IMAGES_UPLOADED가 아닌 상태에서 호출하면 예외 발생")
    void fail_when_not_images_uploaded() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.DRAFT);

      //when & then
      assertThatThrownBy(trip::confirmTrip)
              .isInstanceOf(CustomException.class);
    }
  }

  @Nested
  @DisplayName("softDelete() / isDeleted()")
  class SoftDelete {

    @Test
    @DisplayName("softDelete 호출 전 isDeleted()는 false")
    void before_delete_is_false() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.CONFIRMED);

      //then
      assertThat(trip.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("softDelete 호출 후 deletedAt이 설정된다")
    void after_delete_deleted_at_is_set() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.CONFIRMED);

      //when
      trip.softDelete();

      //then
      assertThat(trip.isDeleted()).isTrue();
      assertThat(trip.getDeletedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("isConfirmed()")
  class IsConfirmed {

    @Test
    @DisplayName("CONFIRMED 상태에서 true 반환")
    void confirmed_returns_true() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.CONFIRMED);

      //then
      assertThat(trip.isConfirmed()).isTrue();
    }

    @Test
    @DisplayName("CONFIRMED가 아닌 상태에서 false 반환")
    void not_confirmed_returns_false() throws Exception {
      //given
      Trip trip = createTrip(TripStatus.DRAFT);

      //then
      assertThat(trip.isConfirmed()).isFalse();
    }
  }
}
