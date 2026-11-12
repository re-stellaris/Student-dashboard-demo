package com.ruoyi.education.assignment.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.education.assignment.domain.EduAssignment;
import com.ruoyi.education.assignment.domain.EduSubmission;
import com.ruoyi.education.assignment.dto.GradeBody;
import com.ruoyi.education.assignment.dto.SubmissionBody;
import com.ruoyi.education.assignment.mapper.EduAssignmentMapper;
import com.ruoyi.education.assignment.mapper.EduSubmissionMapper;
import com.ruoyi.education.assignment.service.IEduAssignmentService;
import com.ruoyi.education.enrollment.domain.EduCourseOffering;
import com.ruoyi.education.enrollment.domain.EduEnrollment;
import com.ruoyi.education.enrollment.domain.EduStudent;
import com.ruoyi.education.enrollment.domain.EduTeacher;
import com.ruoyi.education.enrollment.mapper.EduCourseOfferingMapper;
import com.ruoyi.education.enrollment.mapper.EduEnrollmentMapper;
import com.ruoyi.education.enrollment.support.EduIdentityResolver;

/**
 * 作业与提交 业务实现
 *
 * @author ruoyi-education
 */
@Service
public class EduAssignmentServiceImpl implements IEduAssignmentService
{
    /** 作业状态：草稿 */
    private static final String STATUS_DRAFT = "DRAFT";

    /** 作业状态：已关闭 */
    private static final String STATUS_CLOSED = "CLOSED";

    /** 提交状态：已提交待批改 */
    private static final String SUBMISSION_SUBMITTED = "SUBMITTED";

    /** 选课状态：已退课 */
    private static final String ENROLLMENT_DROPPED = "DROPPED";

    /** 默认满分 */
    private static final BigDecimal DEFAULT_TOTAL_SCORE = new BigDecimal("100.00");

    @Autowired
    private EduAssignmentMapper assignmentMapper;

    @Autowired
    private EduSubmissionMapper submissionMapper;

    @Autowired
    private EduCourseOfferingMapper offeringMapper;

    @Autowired
    private EduEnrollmentMapper enrollmentMapper;

    @Autowired
    private EduIdentityResolver identityResolver;

    @Override
    public List<EduAssignment> selectAssignmentList(EduAssignment query)
    {
        EduAssignment condition = query == null ? new EduAssignment() : query;
        if (identityResolver.isAdmin())
        {
            return assignmentMapper.selectAssignmentList(condition);
        }
        EduTeacher teacher = identityResolver.findTeacher();
        if (teacher != null)
        {
            if (condition.getOfferingId() != null)
            {
                requireTaughtOffering(condition.getOfferingId(), teacher.getTeacherId());
                return assignmentMapper.selectAssignmentList(condition);
            }
            return assignmentMapper.selectAssignmentListByTeacherId(teacher.getTeacherId());
        }
        EduStudent student = identityResolver.findStudent();
        if (student != null)
        {
            if (condition.getOfferingId() != null)
            {
                requireEnrolledOffering(condition.getOfferingId(), student.getStudentId());
            }
            return assignmentMapper.selectAssignmentListByStudentId(student.getStudentId(),
                    condition.getOfferingId());
        }
        throw identityResolver.forbidden();
    }

    @Override
    public EduAssignment selectAssignmentById(Long assignmentId)
    {
        if (assignmentId == null)
        {
            throw new ServiceException("作业ID不能为空");
        }
        EduAssignment assignment = assignmentMapper.selectAssignmentById(assignmentId);
        if (assignment == null)
        {
            throw new ServiceException("作业不存在");
        }
        if (identityResolver.isAdmin())
        {
            return assignment;
        }
        EduTeacher teacher = identityResolver.findTeacher();
        if (teacher != null)
        {
            requireTaughtOffering(assignment.getOfferingId(), teacher.getTeacherId());
            return assignment;
        }
        EduStudent student = identityResolver.findStudent();
        if (student != null)
        {
            requireEnrolledOffering(assignment.getOfferingId(), student.getStudentId());
            if (STATUS_DRAFT.equals(assignment.getStatus()))
            {
                throw identityResolver.forbidden();
            }
            return assignment;
        }
        throw identityResolver.forbidden();
    }

    @Override
    public int insertAssignment(EduAssignment assignment)
    {
        if (assignment == null || assignment.getOfferingId() == null)
        {
            throw new ServiceException("开课不能为空");
        }
        if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty())
        {
            throw new ServiceException("作业标题不能为空");
        }
        EduCourseOffering offering = offeringMapper.selectOfferingById(assignment.getOfferingId());
        if (offering == null)
        {
            throw new ServiceException("开课记录不存在");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            if (offering.getTeacherId() == null || !offering.getTeacherId().equals(teacher.getTeacherId()))
            {
                throw identityResolver.forbidden();
            }
        }
        if (assignment.getStatus() == null || assignment.getStatus().isEmpty())
        {
            assignment.setStatus(STATUS_DRAFT);
        }
        if (assignment.getTotalScore() == null)
        {
            assignment.setTotalScore(DEFAULT_TOTAL_SCORE);
        }
        return assignmentMapper.insertAssignment(assignment);
    }

    @Override
    public int updateAssignment(EduAssignment assignment)
    {
        if (assignment == null || assignment.getAssignmentId() == null)
        {
            throw new ServiceException("作业ID不能为空");
        }
        EduAssignment current = assignmentMapper.selectAssignmentById(assignment.getAssignmentId());
        if (current == null)
        {
            throw new ServiceException("作业不存在");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            requireTaughtOffering(current.getOfferingId(), teacher.getTeacherId());
            if (assignment.getOfferingId() != null
                    && !assignment.getOfferingId().equals(current.getOfferingId()))
            {
                throw identityResolver.forbidden();
            }
        }
        return assignmentMapper.updateAssignment(assignment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAssignmentByIds(Long[] assignmentIds)
    {
        if (assignmentIds == null || assignmentIds.length == 0)
        {
            throw new ServiceException("作业ID不能为空");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            for (Long assignmentId : assignmentIds)
            {
                EduAssignment current = assignmentMapper.selectAssignmentById(assignmentId);
                if (current == null)
                {
                    throw new ServiceException("作业不存在");
                }
                requireTaughtOffering(current.getOfferingId(), teacher.getTeacherId());
            }
        }
        return assignmentMapper.deleteAssignmentByIds(assignmentIds);
    }

    @Override
    public List<EduSubmission> selectSubmissionList(EduSubmission query)
    {
        EduSubmission condition = query == null ? new EduSubmission() : query;
        if (identityResolver.isAdmin())
        {
            return submissionMapper.selectSubmissionList(condition);
        }
        EduTeacher teacher = identityResolver.findTeacher();
        if (teacher != null)
        {
            if (condition.getAssignmentId() != null)
            {
                EduAssignment assignment = assignmentMapper.selectAssignmentById(condition.getAssignmentId());
                if (assignment == null)
                {
                    throw new ServiceException("作业不存在");
                }
                requireTaughtOffering(assignment.getOfferingId(), teacher.getTeacherId());
                return submissionMapper.selectSubmissionList(condition);
            }
            return submissionMapper.selectSubmissionListByTeacherId(teacher.getTeacherId());
        }
        EduStudent student = identityResolver.findStudent();
        if (student != null)
        {
            // 学生只能看到本人提交，身份锚点由服务端强制覆盖
            condition.setStudentId(student.getStudentId());
            return submissionMapper.selectSubmissionList(condition);
        }
        throw identityResolver.forbidden();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitAssignment(SubmissionBody body)
    {
        if (body == null || body.getAssignmentId() == null)
        {
            throw new ServiceException("作业ID不能为空");
        }
        boolean noContent = body.getContent() == null || body.getContent().trim().isEmpty();
        boolean noAttachment = body.getAttachmentUrl() == null || body.getAttachmentUrl().trim().isEmpty();
        if (noContent && noAttachment)
        {
            throw new ServiceException("提交内容与附件不能同时为空");
        }
        EduStudent student = identityResolver.currentStudent();
        EduAssignment assignment = assignmentMapper.selectAssignmentById(body.getAssignmentId());
        if (assignment == null)
        {
            throw new ServiceException("作业不存在");
        }
        if (STATUS_DRAFT.equals(assignment.getStatus()))
        {
            throw new ServiceException("作业尚未发布，无法提交");
        }
        if (STATUS_CLOSED.equals(assignment.getStatus()))
        {
            throw new ServiceException("作业已关闭，无法提交");
        }
        EduEnrollment enrollment = enrollmentMapper.selectByStudentAndOffering(student.getStudentId(),
                assignment.getOfferingId());
        if (enrollment == null || ENROLLMENT_DROPPED.equals(enrollment.getStatus()))
        {
            throw identityResolver.forbidden();
        }

        EduSubmission existing = submissionMapper.selectByAssignmentAndStudent(assignment.getAssignmentId(),
                student.getStudentId());
        if (existing != null)
        {
            EduSubmission update = new EduSubmission();
            update.setSubmissionId(existing.getSubmissionId());
            update.setContent(body.getContent());
            update.setAttachmentUrl(body.getAttachmentUrl());
            update.setSubmittedAt(DateUtils.getNowDate());
            update.setStatus(SUBMISSION_SUBMITTED);
            update.setUpdateBy(identityResolver.currentUsername());
            submissionMapper.updateSubmission(update);
            return existing.getSubmissionId();
        }

        EduSubmission submission = new EduSubmission();
        submission.setAssignmentId(assignment.getAssignmentId());
        submission.setStudentId(student.getStudentId());
        submission.setContent(body.getContent());
        submission.setAttachmentUrl(body.getAttachmentUrl());
        submission.setSubmittedAt(DateUtils.getNowDate());
        submission.setStatus(SUBMISSION_SUBMITTED);
        submission.setCreateBy(identityResolver.currentUsername());
        submissionMapper.insertSubmission(submission);
        return submission.getSubmissionId();
    }

    @Override
    public int gradeSubmission(GradeBody body)
    {
        if (body == null || body.getSubmissionId() == null)
        {
            throw new ServiceException("提交ID不能为空");
        }
        if (body.getGrade() == null)
        {
            throw new ServiceException("得分不能为空");
        }
        EduSubmission submission = submissionMapper.selectSubmissionById(body.getSubmissionId());
        if (submission == null)
        {
            throw new ServiceException("提交记录不存在");
        }
        EduAssignment assignment = assignmentMapper.selectAssignmentById(submission.getAssignmentId());
        if (assignment == null)
        {
            throw new ServiceException("作业不存在");
        }
        if (!identityResolver.isAdmin())
        {
            EduTeacher teacher = identityResolver.currentTeacher();
            requireTaughtOffering(assignment.getOfferingId(), teacher.getTeacherId());
        }
        if (assignment.getTotalScore() != null && body.getGrade().compareTo(assignment.getTotalScore()) > 0)
        {
            throw new ServiceException("得分不能超过作业满分");
        }
        String feedback = body.getFeedback() == null ? "" : body.getFeedback();
        return submissionMapper.updateGrade(body.getSubmissionId(), body.getGrade(), feedback,
                identityResolver.currentUsername());
    }

    /**
     * 校验开课确由指定教师授课
     *
     * @param offeringId 开课ID
     * @param teacherId 当前教师ID
     */
    private void requireTaughtOffering(Long offeringId, Long teacherId)
    {
        if (offeringId == null)
        {
            throw identityResolver.forbidden();
        }
        EduCourseOffering offering = offeringMapper.selectOfferingById(offeringId);
        if (offering == null || offering.getTeacherId() == null || !offering.getTeacherId().equals(teacherId))
        {
            throw identityResolver.forbidden();
        }
    }

    /**
     * 校验学生确已选修该开课（且未退课）
     *
     * @param offeringId 开课ID
     * @param studentId 当前学生ID
     */
    private void requireEnrolledOffering(Long offeringId, Long studentId)
    {
        if (offeringId == null)
        {
            throw identityResolver.forbidden();
        }
        EduEnrollment enrollment = enrollmentMapper.selectByStudentAndOffering(studentId, offeringId);
        if (enrollment == null || ENROLLMENT_DROPPED.equals(enrollment.getStatus()))
        {
            throw identityResolver.forbidden();
        }
    }
}
