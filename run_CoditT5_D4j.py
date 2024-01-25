from transformers import T5ForConditionalGeneration, AutoTokenizer
import os
import subprocess
import shutil
import sys



result = subprocess.run(['pwd'], stdout=subprocess.PIPE, text=True)
root_path = result.stdout.strip()
d4j_path = root_path + "/d4j/"
if (not os.path.exists(d4j_path) or not os.path.isdir(d4j_path)):
    os.makedirs(d4j_path)
output_path = root_path + "/output/"
if (not os.path.exists(output_path) or not os.path.isdir(output_path)):
    os.makedirs(output_path)


def get_bug_info(d4j_project, bid):
    bug_info_file_path = root_path + "/Defects4J_bugs_info/" + d4j_project + ".csv"
    try:
        with open(bug_info_file_path, 'r', encoding='utf-8') as file:
            lines = file.readlines()
            
            for line in lines:
                if line.split(",")[0] == bid:
                    bug_info = line.split(",")
                    return bug_info

    except FileNotFoundError:
        print(f"파일을 찾을 수 없습니다: {bug_info_file_path}")
    except Exception as e:
        print(f"오류 발생: {e}")
    
    print("Wrong Bug ID")
    sys.exit()

def make_d4j_project(d4j_project, bid):
    buggy_project_name = d4j_project + bid + "_buggy"
    command = "defects4j checkout -p " + d4j_project + " -v " + bid + "b -w " + d4j_path + d4j_project + bid
    os.system(command)

def read_buggyline(buggy_line_number, output_file_path, line_range):
    try:
        with open(output_file_path, 'r', encoding='utf-8') as file:
            # 파일을 열어서 특정 라인을 읽어옴
            lines = file.readlines()
        if buggy_line_number - line_range < 0:
            min_line_num = 0
        else:
            min_line_num = buggy_line_number - line_range
        if buggy_line_number + line_range < len(lines) - 1:
            max_line_num = buggy_line_number + line_range
        else:
            max_line_num = len(lines)-1

        target_line = ""
        for i in range(min_line_num, max_line_num + 1):
            target_line += lines[i].strip()
        
        return target_line
    
    except FileNotFoundError:
        print(f"파일을 찾을 수 없습니다: {output_file_path}")
    except Exception as e:
        print(f"오류 발생: {e}")

def replace_token(original_string):
    tokens = ["<INSERT>", "<INSERT_END>", "<REPLACE_OLD>", "<REPLACE_NEW>", "<s>"]
    for t in tokens:
        result_string = original_string.replace(t, "ㅗ")
    return result_string

def run_CoditT5(buggy_code):
    # Make Fixed project using CoditT5
    checkpoint = "JiyangZhang/CoditT5"

    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    model = T5ForConditionalGeneration.from_pretrained(checkpoint)

    input_ids = tokenizer(buggy_code, return_tensors="pt").input_ids
    generated_ids = model.generate(input_ids, max_length=200)
    fixed_code = tokenizer.decode(generated_ids[0], skip_special_tokens=True)
    replaced_code = replace_token(fixed_code)

    return replaced_code.split("ㅗ")[-1]

    # Validate
    # compile_result = subprocess.run("defects4j compile", shell=True)
    # test_result = subprocess.run("defects4j test", shell=True)
    # os.chdir("..")

def replace_bug_With_fix(buggy_line_number, output_file_path, line_range, fixed_code):
    try:
        with open(output_file_path, 'r', encoding='utf-8') as file:
            # 파일을 열어서 특정 라인을 읽어옴
            lines = file.readlines()
        if buggy_line_number - line_range < 0:
            min_line_num = 0
        else:
            min_line_num = buggy_line_number - line_range
        if buggy_line_number + line_range < len(lines) - 1:
            max_line_num = buggy_line_number + line_range
        else:
            max_line_num = len(lines)-1

        for i in range(min_line_num, max_line_num + 1):
            lines[i] = ""
        
        lines[buggy_line_number] = fixed_code
        with open(output_file_path, 'w', encoding='utf-8') as file:
            file.writelines(lines)
        
    except FileNotFoundError:
        print(f"파일을 찾을 수 없습니다: {output_file_path}")
    except Exception as e:
        print(f"오류 발생: {e}")
        
if __name__ == "__main__":
    if len(sys.argv) == 3:
        d4j_project = sys.argv[1]
        bid = sys.argv[2]
    elif len(sys.argv) == 2:
        print("Insufficient arguments")
        sys.exit()
    elif len(sys.argv) == 1: # Default
        print("Default bug is Closure-14")
        d4j_project = "Closure"
        bid = "14"
    
    bug_info = get_bug_info(d4j_project, bid) # Defects4J ID,Faulty file path,fix faulty line,blame faulty line
    #print(bug_info)
    make_d4j_project(d4j_project,bid)
    for i in range(10):
        candidate_path = output_path + "candidates/" # + str(i) + "/"
        if (os.path.exists(candidate_path)):
            shutil.rmtree(candidate_path)
        os.makedirs(candidate_path)
        
        buggy_line_number = int(bug_info[2]) - 1
        buggy_code_path = candidate_path + d4j_project + "-" + bid + "_c" + str(i) + "_old.java"
        fixed_code_path = candidate_path + d4j_project + "-" + bid + "_c" + str(i) + "_new.java"
        shutil.copyfile(d4j_path + d4j_project + bid + "/" + bug_info[1], buggy_code_path)
        shutil.copyfile(d4j_path + d4j_project + bid + "/" + bug_info[1], fixed_code_path)
        buggy_code = read_buggyline(buggy_line_number, fixed_code_path, i)
        fixed_code = run_CoditT5(buggy_code)
        replace_bug_With_fix(buggy_line_number, fixed_code_path, i, fixed_code)

