import { z } from 'zod';

// 서버가 필드 단위 검증 에러를 주지 않으므로(INVALID_INPUT 하나뿐) 클라이언트 검증이 1차 방어선이다.
// 규칙은 docs/03-API/API-에러코드.md 의 "클라이언트 검증 규칙"과 일치시킨다.

export const signupSchema = z
  .object({
    email: z.string().min(1, '이메일을 입력하세요').email('올바른 이메일 형식이 아닙니다'),
    password: z.string().min(8, '비밀번호는 8자 이상이어야 합니다').max(30, '비밀번호는 30자 이하여야 합니다'),
    passwordConfirm: z.string().min(1, '비밀번호를 다시 입력하세요'),
    nickName: z.string().min(1, '닉네임을 입력하세요'),
  })
  .refine((v) => v.password === v.passwordConfirm, {
    message: '비밀번호가 일치하지 않습니다',
    path: ['passwordConfirm'],
  });

export const loginSchema = z.object({
  email: z.string().min(1, '이메일을 입력하세요'),
  password: z.string().min(8, '비밀번호는 8자 이상이어야 합니다'),
});

export const postSchema = z.object({
  title: z.string().min(5, '제목은 5자 이상 200자 이하로 입력하세요').max(200, '제목은 5자 이상 200자 이하로 입력하세요'),
  body: z.string().min(1, '본문을 입력하세요'),
});

export const boardSchema = z.object({
  name: z.string().min(1, '게시판 이름을 입력하세요'),
  description: z.string().optional(),
});

export const commentSchema = z.object({
  content: z.string().min(1, '내용을 입력하세요').max(1000, '댓글은 1000자 이하로 입력하세요'),
});
