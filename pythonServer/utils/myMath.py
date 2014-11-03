import math

def totalMass(numbers):
   if len(numbers) == 0:
      return 0
   largest = max(numbers)
   tot = 0
   for num in numbers:
      tot += math.exp(num - largest)
   return largest + math.log(tot)
