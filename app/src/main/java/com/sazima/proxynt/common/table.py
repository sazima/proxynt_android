from java import jclass

def getPerson():
    # 此处可以在studio中把log调成verbose， 过滤填写stdout
    print ("---java 调用了此方法---")
    # 填写java been的路径
    JavaBean = jclass("com.zgy.python_native.been.Person")
    person = JavaBean()
    person.setName("神气小风")
    person.setAge(25)
    return person

def getPerson(parameter):
    # 此处可以在studio中把log调成verbose， 过滤填写stdout
    print ("---java 调用了此方法---")
    # 填写java been的路径
    JavaBean = jclass("com.zgy.python_native.been.Person")
    person = JavaBean()
    person.setName("神气小风")
    age = parameter + 10
    person.setAge(age)
    return person

def getNumpyData():
    return [1, 3, 3,]
#     arr = np.array([0.25,  1.33,  1,  100])
#     list = np.reciprocal(arr)
#     print (list)
#     return list