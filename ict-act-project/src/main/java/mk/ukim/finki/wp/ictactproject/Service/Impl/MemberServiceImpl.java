package mk.ukim.finki.wp.ictactproject.Service.Impl;

import mk.ukim.finki.wp.ictactproject.Models.Member;
import mk.ukim.finki.wp.ictactproject.Models.Position;
import mk.ukim.finki.wp.ictactproject.Models.PositionType;
import mk.ukim.finki.wp.ictactproject.Models.exceptions.*;
import mk.ukim.finki.wp.ictactproject.Repository.MemberRepository;
import mk.ukim.finki.wp.ictactproject.Repository.PositionRepository;
import mk.ukim.finki.wp.ictactproject.Service.MemberService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(MemberRepository memberRepository, PositionRepository positionRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.positionRepository = positionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static boolean patternMatches(String emailAddress) {
        return !Pattern.compile("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                        + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")
                .matcher(emailAddress)
                .matches();
    }

    @Override
    public List<Member> getAll() {
        return memberRepository.findAll().stream().sorted(Member.SORT_BY_NAME).toList();
    }

    @Override
    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(MemberDoesNotExist::new);
    }

    @Override
    public Member register(String email, String password, String repeatPassword, String name, String surname, String institution, PositionType role) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            throw new InvalidEmailOrPasswordException();
        }

        if (!password.equals(repeatPassword)) {
            throw new PasswordDoNotMatchException();
        }

        if (this.memberRepository.findByEmail(email).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }

        if (patternMatches(email)){
            throw new InvalidUsernameException();
        }

        Member member = new Member(email, passwordEncoder.encode(password), name, surname, institution, PositionType.NEW_USER);
        member.setEnabled(false);
        return memberRepository.save(member);
    }

    @Override
    public Member deleteMember(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(MemberDoesNotExist::new);
        memberRepository.deleteById(id);

        return member;
    }

    @Override
    public Member editMember(Long id, String name, String surname, String institution, PositionType role) {
        Member member = memberRepository.findById(id).orElseThrow(MemberDoesNotExist::new);
        member.setName(name);
        member.setSurname(surname);
        member.setInstitution(institution);

        if (member.getRole() != role) {
            member.setRole(role);
            List<Position> positions = member.getPositions();

            if (positions.isEmpty()) {
                Position newPosition = new Position(role, LocalDate.now(), member);
                positionRepository.save(newPosition);
                positions.add(newPosition);
            } else {
                Position lastPosition = positions.get(positions.size() - 1);
                lastPosition.setToDate(LocalDate.now());
                positionRepository.save(lastPosition);

                Position newPosition = new Position(role, LocalDate.now(), member);
                positionRepository.save(newPosition);
                positions.add(newPosition);
            }
            member.setPositions(positions);
        }

        return memberRepository.save(member);
    }

    @Override
    public List<Member> getMultipleByIds(List<Long> ids) {
        if (ids == null) return new ArrayList<>();
        List<Member> members = new ArrayList<>();
        for (Long id : ids) {
            Member member = findById(id);
            if (member != null) members.add(member);
        }
        return members;
    }

    @Override
    public List<Position> getPositionsByMember(Long id) {
        Member member = findById(id);

        return member.getPositions().stream().sorted(Position.COMPARATOR).toList();
    }

    @Override
    public Member addPosition(Long member_id, PositionType positionType, LocalDate dateFrom, LocalDate dateTo) {
        Member member = findById(member_id);
        Position position;
        if (dateTo != null)
            position = new Position(positionType, dateFrom, dateTo, member);
        else
            position = new Position(positionType, dateFrom, member);
        position = positionRepository.save(position);
        member.getPositions().add(position);
        return memberRepository.save(member);
    }

    @Override
    public Member editPosition(Long member_id, Long position_id, PositionType positionType, LocalDate dateFrom, LocalDate dateTo) {
        Member member = findById(member_id);
        Position position = positionRepository.findById(position_id).orElseThrow(PositionDoesNotExist::new);
        member.getPositions().remove(position);
        position.setPositionType(positionType);
        position.setFromDate(dateFrom);
        if (dateTo != null)
            position.setToDate(dateTo);
        position = positionRepository.save(position);
        member.getPositions().add(position);
        return memberRepository.save(member);
    }

    @Override
    public Member deletePosition(Long member_id, Long position_id) {
        Member member = findById(member_id);
        Position position = positionRepository.findById(position_id).orElseThrow(PositionDoesNotExist::new);
        member.getPositions().remove(position);
        positionRepository.delete(position);
        return memberRepository.save(member);
    }

    @Override
    public List<String> getEmailsByRole(List<PositionType> roles) {
        return memberRepository.findEmailsByRoleIn(roles);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
