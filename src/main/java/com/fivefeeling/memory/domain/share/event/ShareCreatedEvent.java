package com.fivefeeling.memory.domain.share.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareCreatedEvent {

  private Long shareId;
  private Long tripId;
  private Long recipientId;
  private String senderNickname;
}
