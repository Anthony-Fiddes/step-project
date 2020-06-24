// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;

public final class FindMeetingQuery {

  /**
   * Finds available time ranges for all requested attendees to attend the
   * supplied MeetingRequest, given known events
   * 
   * @param events  All known events that will be considered
   * @param request The meeting request whose attendees must all be free
   * @return A collection of time ranges where all proposed attendees in @request
   *         are not attending any known events in @events
   */
  public Collection<TimeRange> query(final Collection<Event> events, final MeetingRequest request) {
    Collection<TimeRange> queryRanges = new ArrayList<>();
    queryRanges.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, true));
    Map<Event, Boolean> cache = new HashMap<>();
    for (Event event : events) {
      Collection<TimeRange> nextRanges = new ArrayList<>();
      for (TimeRange range : queryRanges) {
        if (hasOverlappingAttendees(event, request, cache)) {
          nextRanges.addAll(splitToAvailableTimeRanges(range, event.getWhen()));
        } else {
          nextRanges.add(range);
        }
      }
      queryRanges = nextRanges;
    }
    List<TimeRange> resultRanges = new ArrayList<>();
    for (TimeRange range : queryRanges) {
      if (range.duration() >= request.getDuration()) {
        resultRanges.add(range);
      }
    }
    Collections.sort(resultRanges, TimeRange.ORDER_BY_END);
    return resultRanges;
  }

  private boolean hasOverlappingAttendees(Event event, MeetingRequest request, Map<Event, Boolean> cache) {
    Boolean isOverlapping = cache.get(event);
    if (isOverlapping == null) {
      isOverlapping = hasOverlappingAttendees(event, request);
      cache.put(event, isOverlapping);
    }
    return isOverlapping;
  }

  private boolean hasOverlappingAttendees(Event event, MeetingRequest request) {
    Set<String> eventAttendees = event.getAttendees();
    Iterator<String> reqAttendeesIter = request.getAttendees().iterator();
    while (reqAttendeesIter.hasNext()) {
      String attendee = reqAttendeesIter.next();
      if (eventAttendees.contains(attendee)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes any time from @range where @eventTime overlaps it.
   * 
   * @param range     The TimeRange from which available time will be selected.
   * @param eventTime The TimeRange that is considered to be occupied.
   * @return All TimeRange from @range where @eventTime is not occurring
   */
  private Collection<TimeRange> splitToAvailableTimeRanges(TimeRange range, TimeRange eventTime) {
    Collection<TimeRange> availableRanges = new ArrayList<>();
    if (!eventTime.overlaps(range)) {
      availableRanges.add(range);
      return availableRanges;
    }
    if (eventTime.contains(range)) {
      return availableRanges;
    }

    TimeRange availableRange;
    if (eventTime.end() > range.start() || range.end() > eventTime.end()) {
      availableRange = TimeRange.fromStartEnd(eventTime.end(), range.end(), false);
      availableRanges.add(availableRange);
    }
    if (range.end() > eventTime.start()) {
      availableRange = TimeRange.fromStartEnd(range.start(), eventTime.start(), false);
      availableRanges.add(availableRange);
    }
    return availableRanges;
  }
}
