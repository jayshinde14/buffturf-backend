package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.*;
import com.buffturf.buffturf_backend.repository.*;
import com.buffturf.buffturf_backend.service.EmailService;
import com.buffturf.buffturf_backend.service.QrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QrPassRepository qrPassRepository;
    @Mock
    private QrService qrService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Turf testTurf;
    private Slot testSlot1;
    private Slot testSlot2;
    private BookingRequest bookingRequest;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setBanned(false);

        testTurf = new Turf();
        testTurf.setId(1L);
        testTurf.setName("Test Turf");
        testTurf.setPricePerHour(1000.0);

        testSlot1 = new Slot();
        testSlot1.setId(1L);
        testSlot1.setStartTime(LocalTime.of(10, 0));
        testSlot1.setEndTime(LocalTime.of(11, 0));
        testSlot1.setIsAvailable(true);

        testSlot2 = new Slot();
        testSlot2.setId(2L);
        testSlot2.setStartTime(LocalTime.of(11, 0));
        testSlot2.setEndTime(LocalTime.of(12, 0));
        testSlot2.setIsAvailable(true);

        bookingRequest = new BookingRequest();
        bookingRequest.setTurfId(1L);
        bookingRequest.setSlotIds(Arrays.asList(1L, 2L));
        bookingRequest.setBookingDate(LocalDate.now().plusDays(1)); // Tomorrow

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUser(testUser);
        testBooking.setTurf(testTurf);
        testBooking.setSlots(Arrays.asList(testSlot1, testSlot2));
        testBooking.setBookingDate(LocalDate.now().plusDays(1));
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        testBooking.setBookingCode("BUFF-TEST1234");
    }

    // --- createBooking Tests ---

    @Test
    void createBooking_UserNotFound_ThrowsException() {
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
    }

    @Test
    void createBooking_UserBanned_ThrowsException() {
        testUser.setBanned(true);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void createBooking_TurfNotFound_ThrowsException() {
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
    }

    @Test
    void createBooking_SlotsNotFound_ThrowsException() {
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Collections.singletonList(testSlot1)); // Size mismatch

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("One or more slots not found", ex.getMessage());
    }

    @Test
    void createBooking_SlotsNotContinuous_ThrowsException() {
        testSlot2.setStartTime(LocalTime.of(12, 0)); // Gap between slots
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Arrays.asList(testSlot1, testSlot2));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Selected slots must be continuous (back-to-back).", ex.getMessage());
    }

    @Test
    void createBooking_SlotAlreadyBooked_ThrowsException() {
        testSlot1.setIsAvailable(false);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Arrays.asList(testSlot1, testSlot2));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("One or more slots are already booked!", ex.getMessage());
    }

    @Test
    void createBooking_SlotInPast_ThrowsException() {
        bookingRequest.setBookingDate(LocalDate.now().minusDays(1)); // Yesterday
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Arrays.asList(testSlot1, testSlot2));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.createBooking(bookingRequest, "test@test.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Cannot book a slot in the past", ex.getMessage());
    }

    @Test
    void createBooking_Success_WithPlayers() {
        BookingRequest.PlayerRequest player1 = new BookingRequest.PlayerRequest();
        player1.setName("Player 1");
        BookingRequest.PlayerRequest player2 = new BookingRequest.PlayerRequest();
        player2.setName("Player 2");
        bookingRequest.setPlayers(Arrays.asList(player1, player2));

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Arrays.asList(testSlot1, testSlot2));
        
        given(bookingRepository.save(any(Booking.class))).willAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L); // Simulate save
            return b;
        });

        given(qrService.generateQrToken(any(), any(), any())).willReturn("test-qr-token");

        Booking savedBooking = bookingService.createBooking(bookingRequest, "test@test.com");

        assertNotNull(savedBooking);
        assertEquals(testUser, savedBooking.getUser());
        assertEquals(Booking.BookingStatus.CONFIRMED, savedBooking.getStatus());
        assertEquals(2, savedBooking.getParticipants().size());
        assertFalse(testSlot1.getIsAvailable());
        assertFalse(testSlot2.getIsAvailable());

        verify(slotRepository).saveAll(anyList());
        verify(qrPassRepository, times(2)).save(any(QrPass.class)); // 2 players
        verify(emailService).sendBookingConfirmation(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(), anyDouble());
    }

    @Test
    void createBooking_Success_WithoutPlayers() {
        bookingRequest.setPlayers(null);

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Arrays.asList(testSlot1, testSlot2));
        
        given(bookingRepository.save(any(Booking.class))).willAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        given(qrService.generateQrToken(any(), any(), any())).willReturn("test-qr-token");

        Booking savedBooking = bookingService.createBooking(bookingRequest, "test@test.com");

        assertNotNull(savedBooking);
        assertEquals(1, savedBooking.getParticipants().size());
        assertEquals(testUser.getUsername(), savedBooking.getParticipants().get(0).getFullName());

        verify(slotRepository).saveAll(anyList());
        verify(qrPassRepository, times(1)).save(any(QrPass.class)); // Default fallback player
        verify(emailService).sendBookingConfirmation(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(), anyDouble());
    }

    // --- createBookingWithPayment Tests ---

    @Test
    void createBookingWithPayment_Success() {
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(bookingRequest.getSlotIds())).willReturn(Arrays.asList(testSlot1, testSlot2));
        
        given(bookingRepository.save(any(Booking.class))).willAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        given(qrService.generateQrToken(any(), any(), any())).willReturn("test-qr-token");

        Booking savedBooking = bookingService.createBookingWithPayment(bookingRequest, "test@test.com", "pay_123");

        assertNotNull(savedBooking);
        assertEquals("pay_123", savedBooking.getPaymentId());
        assertEquals("PAID", savedBooking.getPaymentStatus());
        // verify save is called twice: once in createBooking, once in createBookingWithPayment
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    // --- getMyBookings Tests ---

    @Test
    void getMyBookings_UserNotFound_ThrowsException() {
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.getMyBookings("test@test.com"));
    }

    @Test
    void getMyBookings_Success() {
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(bookingRepository.findByUserId(1L)).willReturn(Collections.singletonList(testBooking));

        List<Booking> bookings = bookingService.getMyBookings("test@test.com");

        assertEquals(1, bookings.size());
        assertEquals(testBooking, bookings.get(0));
    }

    // --- cancelBooking Tests ---

    @Test
    void cancelBooking_BookingNotFound_ThrowsException() {
        given(bookingRepository.findById(1L)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.cancelBooking(1L, "testuser"));
    }

    @Test
    void cancelBooking_NotAuthorized_ThrowsException() {
        given(bookingRepository.findById(1L)).willReturn(Optional.of(testBooking));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.cancelBooking(1L, "otheruser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void cancelBooking_Success() {
        Participant p = new Participant();
        QrPass qr = new QrPass();
        qr.setScanStatus(QrPass.ScanStatus.PENDING);
        p.setQrPass(qr);
        testBooking.setParticipants(Collections.singletonList(p));
        testSlot1.setIsAvailable(false);
        testSlot2.setIsAvailable(false);

        given(bookingRepository.findById(1L)).willReturn(Optional.of(testBooking));

        bookingService.cancelBooking(1L, "testuser");

        assertEquals(Booking.BookingStatus.CANCELLED, testBooking.getStatus());
        assertTrue(testSlot1.getIsAvailable());
        assertTrue(testSlot2.getIsAvailable());
        assertEquals(QrPass.ScanStatus.CANCELLED, qr.getScanStatus());

        verify(slotRepository).saveAll(testBooking.getSlots());
        verify(bookingRepository).save(testBooking);
    }

    // --- verifyBooking Tests ---

    @Test
    void verifyBooking_BookingNotFound_ThrowsException() {
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.verifyBooking("CODE"));
    }

    @Test
    void verifyBooking_CancelledStatus() {
        testBooking.setStatus(Booking.BookingStatus.CANCELLED);
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        Map<String, Object> result = bookingService.verifyBooking("CODE");
        assertEquals("INVALID", result.get("validationStatus"));
    }

    @Test
    void verifyBooking_CompletedStatus() {
        testBooking.setStatus(Booking.BookingStatus.COMPLETED);
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        Map<String, Object> result = bookingService.verifyBooking("CODE");
        assertEquals("ALREADY_USED", result.get("validationStatus"));
    }

    @Test
    void verifyBooking_ExpiredDate() {
        testBooking.setBookingDate(LocalDate.now().minusDays(1)); // Past date
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        Map<String, Object> result = bookingService.verifyBooking("CODE");
        assertEquals("INVALID", result.get("validationStatus"));
    }

    @Test
    void verifyBooking_FutureDate() {
        testBooking.setBookingDate(LocalDate.now().plusDays(1)); // Future date
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        Map<String, Object> result = bookingService.verifyBooking("CODE");
        assertEquals("FUTURE_BOOKING", result.get("validationStatus"));
    }

    @Test
    void verifyBooking_TooEarly() {
        try (org.mockito.MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS);
             org.mockito.MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            
            LocalDate fixedToday = LocalDate.of(2024, 1, 1);
            LocalTime fixedNow = LocalTime.of(12, 0);
            mockedDate.when(LocalDate::now).thenReturn(fixedToday);
            mockedTime.when(LocalTime::now).thenReturn(fixedNow);

            testBooking.setBookingDate(fixedToday);
            testSlot1.setStartTime(LocalTime.of(14, 0)); // 2 hours from now
            testSlot1.setEndTime(LocalTime.of(15, 0));
            testBooking.setSlots(Collections.singletonList(testSlot1));
            
            given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

            Map<String, Object> result = bookingService.verifyBooking("CODE");
            assertEquals("TIME_CONFLICT", result.get("validationStatus"));
            assertTrue(result.get("validationMessage").toString().contains("Too early"));
        }
    }

    @Test
    void verifyBooking_TooLate() {
        try (org.mockito.MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS);
             org.mockito.MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            
            LocalDate fixedToday = LocalDate.of(2024, 1, 1);
            LocalTime fixedNow = LocalTime.of(12, 0);
            mockedDate.when(LocalDate::now).thenReturn(fixedToday);
            mockedTime.when(LocalTime::now).thenReturn(fixedNow);

            testBooking.setBookingDate(fixedToday);
            testSlot1.setStartTime(LocalTime.of(9, 0)); // 3 hours ago
            testSlot1.setEndTime(LocalTime.of(10, 0));
            testBooking.setSlots(Collections.singletonList(testSlot1));
            
            given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

            Map<String, Object> result = bookingService.verifyBooking("CODE");
            assertEquals("TIME_CONFLICT", result.get("validationStatus"));
            assertTrue(result.get("validationMessage").toString().contains("Expired"));
        }
    }

    @Test
    void verifyBooking_Success_WithinGracePeriod() {
        try (org.mockito.MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS);
             org.mockito.MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            
            LocalDate fixedToday = LocalDate.of(2024, 1, 1);
            LocalTime fixedNow = LocalTime.of(12, 0);
            mockedDate.when(LocalDate::now).thenReturn(fixedToday);
            mockedTime.when(LocalTime::now).thenReturn(fixedNow);

            testBooking.setBookingDate(fixedToday);
            testSlot1.setStartTime(LocalTime.of(11, 50)); // started 10 mins ago
            testSlot1.setEndTime(LocalTime.of(12, 50));
            testBooking.setSlots(Collections.singletonList(testSlot1));
            
            given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

            Map<String, Object> result = bookingService.verifyBooking("CODE");
            assertEquals("VALID", result.get("validationStatus"));
        }
    }

    // --- confirmCheckIn Tests ---

    @Test
    void confirmCheckIn_BookingNotFound_ThrowsException() {
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.confirmCheckIn("CODE"));
    }

    @Test
    void confirmCheckIn_Cancelled_ThrowsException() {
        testBooking.setStatus(Booking.BookingStatus.CANCELLED);
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.confirmCheckIn("CODE"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Cannot check in a cancelled booking", ex.getMessage());
    }

    @Test
    void confirmCheckIn_Completed_ThrowsException() {
        testBooking.setStatus(Booking.BookingStatus.COMPLETED);
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        ApiException ex = assertThrows(ApiException.class, () -> 
            bookingService.confirmCheckIn("CODE"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Booking is already checked-in", ex.getMessage());
    }

    @Test
    void confirmCheckIn_Success() {
        given(bookingRepository.findByBookingCode("CODE")).willReturn(Optional.of(testBooking));

        Map<String, Object> result = bookingService.confirmCheckIn("CODE");
        
        assertEquals(Booking.BookingStatus.COMPLETED, testBooking.getStatus());
        assertTrue((Boolean) result.get("success"));
        verify(bookingRepository).save(testBooking);
    }

    // --- Missed Branch Coverage Tests ---

    @Test
    void cancelBooking_WithNullParticipants() {
        testBooking.setParticipants(null);
        given(bookingRepository.findById(1L)).willReturn(Optional.of(testBooking));

        assertDoesNotThrow(() -> bookingService.cancelBooking(1L, "testuser"));
        assertEquals(Booking.BookingStatus.CANCELLED, testBooking.getStatus());
    }

    @Test
    void createBooking_WithPlayerInfo_NullValues_UsesUserFallback() {
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(anyList())).willReturn(Collections.singletonList(testSlot1));
        given(bookingRepository.save(any(Booking.class))).willAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(1L);
            return b;
        });
        
        BookingRequest.PlayerRequest incompletePlayer = new BookingRequest.PlayerRequest();
        incompletePlayer.setName("   "); // Blank name
        incompletePlayer.setGender(null);
        incompletePlayer.setContact(null);
        
        BookingRequest reqWithIncompletePlayer = new BookingRequest();
        reqWithIncompletePlayer.setTurfId(1L);
        reqWithIncompletePlayer.setSlotIds(Collections.singletonList(1L));
        reqWithIncompletePlayer.setBookingDate(LocalDate.now().plusDays(1));
        reqWithIncompletePlayer.setPlayers(Collections.singletonList(incompletePlayer));

        Booking result = bookingService.createBooking(reqWithIncompletePlayer, "test@example.com");

        Participant mainParticipant = result.getParticipants().get(0);
        assertEquals(testUser.getUsername(), mainParticipant.getFullName());
        assertEquals("Male", mainParticipant.getGender());
        assertEquals(testUser.getPhoneNumber(), mainParticipant.getPhone());
    }

    @Test
    void createBooking_EmailServiceThrowsException() {
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
        given(turfRepository.findById(1L)).willReturn(Optional.of(testTurf));
        given(slotRepository.findAllById(anyList())).willReturn(Collections.singletonList(testSlot1));
        given(bookingRepository.save(any(Booking.class))).willAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(1L);
            return b;
        });
        
        // Force email service to throw exception to cover catch block
        doThrow(new RuntimeException("Email failure")).when(emailService)
            .sendBookingConfirmation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyDouble());

        BookingRequest req = new BookingRequest();
        req.setTurfId(1L);
        req.setSlotIds(Collections.singletonList(1L));
        req.setBookingDate(LocalDate.now().plusDays(1));

        assertDoesNotThrow(() -> bookingService.createBooking(req, "test@example.com"));
    }

}
