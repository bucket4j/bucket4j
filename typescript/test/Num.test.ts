import { Num } from '../src/index'

test('add', () => {
  expect(new Num(5).add(new Num(6)).val()).toBe(11)
})

test('toString', () => {
  expect(new Num(5).toString()).toBe('5')
})

test('addAll', () => {
  expect(Num.addAll([new Num(5), new Num(2), new Num(13)]).val()).toBe(20)
})
