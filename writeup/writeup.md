# Phase 3: Investigating Alternative Indexes
1.The third indexs:
I choose the hashmap algorithm as the alternative index. For filtering, since we have the high and lower bound, we just need to search the hash map to see if the key inside the hashmap or not.
2.Run the algorithm
If you want to run it , you could just uncomment the hashmap algorithm in filter and comment the other two different index algorithm to test it.
3.Result
It shows that the result not have significant difference performance.
4.Reason
maybe because the number of records for this test is still too small. In bigger datasets, when the index is sparse in terms of numeric difference, B+-tree should be lot faster than Hash index. However, when index is dense in terms of numeric difference, Hashing should perform better since it has O(1) lookup time.
